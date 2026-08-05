package com.anticucheria.service.impl;

import com.anticucheria.exception.ResourceNotFoundException;
import com.anticucheria.model.Boleta;
import com.anticucheria.model.enums.EstadoSunat;
import com.anticucheria.repository.BoletaRepository;
import com.anticucheria.service.BoletaPublicaService;
import com.anticucheria.service.FactuSmartClientService;
import com.anticucheria.service.factusmart.ArchivoComprobante;
import com.anticucheria.service.factusmart.TipoArchivo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BoletaPublicaServiceImpl implements BoletaPublicaService {

    private final BoletaRepository boletaRepository;
    private final FactuSmartClientService factuSmartClientService;

    @Value("${app.boleta.token-dias-vigencia:30}")
    private int diasVigencia;

    @Override
    @Transactional(readOnly = true)
    public ArchivoComprobante descargarPdfPublico(String token) {
        // Misma respuesta para token inexistente, pendiente o caducado: no filtrar existencia.
        Boleta boleta = boletaRepository.findByTokenPublico(token)
                .orElseThrow(() -> new ResourceNotFoundException("Boleta no encontrada"));

        if (boleta.getEstadoSunat() != EstadoSunat.ACEPTADO || boleta.isSimulada()) {
            throw new ResourceNotFoundException("Boleta no encontrada");
        }
        if (boleta.getEmitidoEn() == null
                || boleta.getEmitidoEn().plusDays(diasVigencia).isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("Boleta no encontrada");
        }

        return factuSmartClientService.descargar(boleta, TipoArchivo.PDF);
    }
}
