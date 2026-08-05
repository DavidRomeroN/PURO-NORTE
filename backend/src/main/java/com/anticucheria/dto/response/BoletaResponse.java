package com.anticucheria.dto.response;

import com.anticucheria.model.enums.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BoletaResponse {

    private Long id;
    private Long pedidoId;
    private String externalId;
    private TipoBoleta tipo;
    private String serie;
    private String correlativo;
    private BigDecimal montoTotal;
    private FormaPago formaPago;
    private MedioPago medioPago;
    private EstadoSunat estadoSunat;
    private String sunatCodigo;
    private String sunatDescripcion;
    private String clienteDocumento;
    private Integer intentosEnvio;
    private LocalDateTime ultimoIntentoEn;
    /** Emitida en modo de prueba: no existe ante SUNAT, aunque figure aceptada. */
    private boolean simulada;
    /** Si existe un PDF que se le pueda dar al cliente. */
    private boolean descargable;
    /** Enlace público (sin JWT) al PDF, solo si SUNAT aceptó. */
    private String urlPublicaPdf;
    private String tokenPublico;
    private LocalDateTime emitidoEn;
    private List<BoletaDetalleResponse> detalles;
}
