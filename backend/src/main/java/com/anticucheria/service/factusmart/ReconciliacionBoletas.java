package com.anticucheria.service.factusmart;

import com.anticucheria.config.FactuSmartConfig;
import com.anticucheria.model.Boleta;
import com.anticucheria.model.enums.EstadoSunat;
import com.anticucheria.repository.BoletaRepository;
import com.anticucheria.service.BoletaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cierra el dia poniendo al dia las boletas que quedaron pendientes.
 *
 * FactuSmart reintenta por su cuenta tres veces al dia, asi que la mayoria de los "01"
 * se resuelven solos; esto solo trae ese resultado a la base local para que el panel no
 * muestre pendientes que en realidad ya fueron aceptadas.
 *
 * Se consulta boleta por boleta en vez de listar las que siguen en "01": el listado dice
 * cuales continuan pendientes, pero no en que quedaron las demas, que es justo el dato
 * que hace falta. Con unas veinte boletas al mes el costo de preguntar una por una es
 * irrelevante, y ademas consultar es gratis.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliacionBoletas {

    private final BoletaRepository boletaRepository;
    private final BoletaService boletaService;
    private final FactuSmartConfig config;

    /** 11pm, despues del cierre del local. */
    @Scheduled(cron = "0 0 23 * * *", zone = "America/Lima")
    public void reconciliar() {
        // Simulando no hay nada que consultar, y preguntar marcaria como simuladas las
        // pendientes que si se emitieron de verdad cuando habia API key.
        if (config.debeSimular()) {
            return;
        }

        List<Boleta> pendientes = boletaRepository.findByEstadoSunatOrderByEmitidoEnDesc(EstadoSunat.PENDIENTE);
        if (pendientes.isEmpty()) {
            return;
        }

        int resueltas = 0;
        for (Boleta boleta : pendientes) {
            if (boleta.getExternalId() == null) {
                continue;
            }
            try {
                if (boletaService.sincronizar(boleta.getId()).getEstadoSunat() != EstadoSunat.PENDIENTE) {
                    resueltas++;
                }
            } catch (RuntimeException ex) {
                log.warn("No se pudo sincronizar la boleta {}: {}", boleta.getId(), ex.getMessage());
            }
        }

        log.info("Reconciliacion diaria: {} de {} boletas pendientes quedaron resueltas",
                resueltas, pendientes.size());
    }
}
