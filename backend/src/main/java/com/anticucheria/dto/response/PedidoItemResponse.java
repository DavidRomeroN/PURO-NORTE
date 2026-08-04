package com.anticucheria.dto.response;

import com.anticucheria.model.enums.EstadoDespacho;
import com.anticucheria.model.enums.TipoItem;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PedidoItemResponse {

    private Long id;
    private TipoItem tipoItem;
    private Long comboId;
    private String comboNombre;
    private Integer cantidad;
    private BigDecimal precioCalculado;
    private BigDecimal precioFinal;
    private Boolean editadoManualmente;
    private String motivoEdicion;
    private Boolean paraLlevar;
    private EstadoDespacho estadoDespacho;
    private LocalDateTime despachadoEn;
    private String observaciones;
    private List<ComponenteResponse> componentes;
}
