package com.anticucheria.repository;

import com.anticucheria.model.Boleta;
import com.anticucheria.model.enums.EstadoSunat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {

    boolean existsByPedidoId(Long pedidoId);

    List<Boleta> findAllByOrderByEmitidoEnDesc();

    List<Boleta> findByEstadoSunatOrderByEmitidoEnDesc(EstadoSunat estadoSunat);

    List<Boleta> findByEmitidoEnBetweenOrderByEmitidoEnDesc(LocalDateTime desde, LocalDateTime hasta);

    List<Boleta> findByEstadoSunatAndEmitidoEnBetweenOrderByEmitidoEnDesc(
            EstadoSunat estadoSunat, LocalDateTime desde, LocalDateTime hasta);
}
