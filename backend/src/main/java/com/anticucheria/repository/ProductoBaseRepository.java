package com.anticucheria.repository;

import com.anticucheria.model.ProductoBase;
import com.anticucheria.model.enums.TipoProducto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoBaseRepository extends JpaRepository<ProductoBase, Long> {

    List<ProductoBase> findByTipoAndActivoTrueOrderByNombreAsc(TipoProducto tipo);

    List<ProductoBase> findByActivoTrueOrderByNombreAsc();

    List<ProductoBase> findByTipoOrderByNombreAsc(TipoProducto tipo);

    List<ProductoBase> findAllByOrderByNombreAsc();

    Optional<ProductoBase> findByNombre(String nombre);
}
