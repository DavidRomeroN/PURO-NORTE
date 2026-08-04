package com.anticucheria.repository;

import com.anticucheria.model.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

    Optional<Mesa> findByNumero(Integer numero);

    boolean existsByNumero(Integer numero);

    List<Mesa> findAllByOrderByNumeroAsc();
}
