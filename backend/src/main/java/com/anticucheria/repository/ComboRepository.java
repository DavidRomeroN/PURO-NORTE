package com.anticucheria.repository;

import com.anticucheria.model.Combo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComboRepository extends JpaRepository<Combo, Long> {

    List<Combo> findByActivoTrueOrderByNombreAsc();

    List<Combo> findAllByOrderByNombreAsc();

    Optional<Combo> findByNombre(String nombre);
}
