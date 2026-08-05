package com.anticucheria.service.impl;

import com.anticucheria.exception.ResourceNotFoundException;
import com.anticucheria.model.Boleta;
import com.anticucheria.model.enums.EstadoSunat;
import com.anticucheria.repository.BoletaRepository;
import com.anticucheria.service.FactuSmartClientService;
import com.anticucheria.service.factusmart.ArchivoComprobante;
import com.anticucheria.service.factusmart.TipoArchivo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoletaPublicaTest {

    @Mock BoletaRepository boletaRepository;
    @Mock FactuSmartClientService factuSmartClientService;

    BoletaPublicaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BoletaPublicaServiceImpl(boletaRepository, factuSmartClientService);
        ReflectionTestUtils.setField(service, "diasVigencia", 30);
    }

    @Test
    void boletaAceptada_generaTokenPublicoUnico() {
        Boleta boleta = Boleta.builder()
                .id(1L)
                .estadoSunat(EstadoSunat.ACEPTADO)
                .simulada(false)
                .emitidoEn(LocalDateTime.now())
                .build();
        // simula lo que hace BoletaServiceImpl.asegurarTokenPublico
        boleta.setTokenPublico(UUID.randomUUID().toString().replace("-", ""));
        assertThat(boleta.getTokenPublico()).hasSize(32);
        assertThat(boleta.getTokenPublico()).isNotEqualTo(
                UUID.randomUUID().toString().replace("-", ""));
    }

    @Test
    void boletaPendiente_endpointPublico404() {
        Boleta boleta = Boleta.builder()
                .estadoSunat(EstadoSunat.PENDIENTE)
                .tokenPublico("abc")
                .emitidoEn(LocalDateTime.now())
                .build();
        when(boletaRepository.findByTokenPublico("abc")).thenReturn(Optional.of(boleta));

        assertThatThrownBy(() -> service.descargarPdfPublico("abc"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void tokenInexistente_404SinFiltrar() {
        when(boletaRepository.findByTokenPublico("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.descargarPdfPublico("nope"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Boleta no encontrada");
    }

    @Test
    void tokenCaducado_404() {
        Boleta boleta = Boleta.builder()
                .estadoSunat(EstadoSunat.ACEPTADO)
                .simulada(false)
                .tokenPublico("viejo")
                .emitidoEn(LocalDateTime.now().minusDays(40))
                .build();
        when(boletaRepository.findByTokenPublico("viejo")).thenReturn(Optional.of(boleta));

        assertThatThrownBy(() -> service.descargarPdfPublico("viejo"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void endpointPublicoDevuelvePdfSinExponerCliente() {
        Boleta boleta = Boleta.builder()
                .estadoSunat(EstadoSunat.ACEPTADO)
                .simulada(false)
                .tokenPublico("tok123")
                .clienteDocumento("12345678")
                .emitidoEn(LocalDateTime.now())
                .build();
        when(boletaRepository.findByTokenPublico("tok123")).thenReturn(Optional.of(boleta));
        when(factuSmartClientService.descargar(eq(boleta), eq(TipoArchivo.PDF)))
                .thenReturn(new ArchivoComprobante(new byte[]{1, 2}, "application/pdf", "boleta.pdf"));

        ArchivoComprobante archivo = service.descargarPdfPublico("tok123");
        assertThat(archivo.nombre()).isEqualTo("boleta.pdf");
        assertThat(archivo.contenido()).containsExactly(1, 2);
        verify(factuSmartClientService).descargar(boleta, TipoArchivo.PDF);
    }
}
