package com.anticucheria.repository;

import com.anticucheria.model.PedidoItemComponente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoItemComponenteRepository extends JpaRepository<PedidoItemComponente, Long> {

    List<PedidoItemComponente> findByPedidoItemId(Long pedidoItemId);
}
