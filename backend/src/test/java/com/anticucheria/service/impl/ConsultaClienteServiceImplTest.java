package com.anticucheria.service.impl;

import com.anticucheria.config.ConsultaDniConfig;
import com.anticucheria.dto.response.ConsultaClienteResponse;
import com.anticucheria.exception.ReglaNegocioException;
import com.anticucheria.model.enums.EstadoConsulta;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("Verificación del DNI en la caja")
class ConsultaClienteServiceImplTest {

    private static final String URL = "https://apiperu.dev/api/dni";
    private static final String DNI = "27427864";

    private ConsultaDniConfig config;
    private MockRestServiceServer servidor;
    private ConsultaClienteServiceImpl servicio;

    @BeforeEach
    void prepararServicio() {
        config = new ConsultaDniConfig();
        config.setUrl(URL);
        config.setToken("un-token-cualquiera");

        RestClient.Builder builder = RestClient.builder()
                .defaultStatusHandler(estado -> true, (peticion, respuesta) -> { });
        servidor = MockRestServiceServer.bindTo(builder).build();

        servicio = new ConsultaClienteServiceImpl(config, builder.build(), new ObjectMapper());
    }

    @Test
    @DisplayName("devuelve el nombre cuando el documento figura")
    void documentoEncontrado() {
        servidor.expect(requestTo(URL))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().json("{\"dni\":\"" + DNI + "\"}"))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "data": {
                            "numero": "27427864",
                            "nombre_completo": "CASTILLO TERRONES, JOSE PEDRO",
                            "nombres": "JOSE PEDRO"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        ConsultaClienteResponse respuesta = servicio.consultar("1", DNI);

        assertThat(respuesta.getEstado()).isEqualTo(EstadoConsulta.ENCONTRADO);
        assertThat(respuesta.getNombreCompleto()).isEqualTo("CASTILLO TERRONES, JOSE PEDRO");
        servidor.verify();
    }

    /**
     * El proveedor responde 404 tanto cuando el número no figura como cuando el token está
     * mal. Lo que los separa es el cuerpo, y la diferencia importa: al cajero hay que
     * decirle "revisa el número" o "no se pudo verificar", que no son lo mismo.
     */
    @Test
    @DisplayName("un 404 con cuerpo del proveedor es un documento que no figura")
    void documentoQueNoFigura() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"success\": false, \"message\": \"No se encontró DNI\"}"));

        ConsultaClienteResponse respuesta = servicio.consultar("1", DNI);

        assertThat(respuesta.getEstado()).isEqualTo(EstadoConsulta.NO_ENCONTRADO);
        assertThat(respuesta.getNombreCompleto()).isNull();
    }

    @Test
    @DisplayName("un token rechazado no se confunde con un DNI inexistente")
    void tokenRechazado() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\": \"Unauthenticated\"}"));

        assertThat(servicio.consultar("1", DNI).getEstado()).isEqualTo(EstadoConsulta.NO_VERIFICADO);
    }

    @Test
    @DisplayName("si el proveedor se cae, la caja puede seguir cobrando")
    void proveedorCaido() {
        servidor.expect(requestTo(URL)).andRespond(withServerError());

        assertThat(servicio.consultar("1", DNI).getEstado()).isEqualTo(EstadoConsulta.NO_VERIFICADO);
    }

    @Test
    @DisplayName("sin token no se gasta una llamada al proveedor")
    void sinTokenNoConsulta() {
        config.setToken("  ");

        assertThat(servicio.consultar("1", DNI).getEstado()).isEqualTo(EstadoConsulta.NO_VERIFICADO);
        servidor.verify(); // no se esperaba ninguna petición
    }

    @Test
    @DisplayName("rechaza números que no son un DNI")
    void numeroInvalido() {
        assertThatThrownBy(() -> servicio.consultar("1", "1234"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("8");
    }

    @Test
    @DisplayName("todavía no se consultan RUC ni otros documentos")
    void tipoNoSoportado() {
        assertThatThrownBy(() -> servicio.consultar("6", "20605577346"))
                .isInstanceOf(ReglaNegocioException.class);
    }
}
