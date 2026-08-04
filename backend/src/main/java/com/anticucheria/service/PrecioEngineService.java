package com.anticucheria.service;

import com.anticucheria.dto.request.SustitucionDTO;

import java.math.BigDecimal;
import java.util.List;

public interface PrecioEngineService {

    BigDecimal calcularPrecioAnticucho(List<Long> productoBaseIds);

    BigDecimal calcularPrecioCombo(Long comboId, List<SustitucionDTO> sustituciones);

    BigDecimal calcularPrecioItemSimple(Long productoBaseId);
}
