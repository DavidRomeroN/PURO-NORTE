package com.anticucheria.repository;

import com.anticucheria.model.ComboSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComboSlotRepository extends JpaRepository<ComboSlot, Long> {

    List<ComboSlot> findByComboIdOrderByOrdenAsc(Long comboId);

    long countByComboId(Long comboId);

    List<ComboSlot> findByProductoBaseDefaultIdAndComboActivoTrue(Long productoBaseDefaultId);
}
