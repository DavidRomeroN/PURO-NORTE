package com.anticucheria.service.agrupacion;

import com.anticucheria.dto.response.PedidoItemResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LineaAgrupada {

    private String clave;
    private String descripcion;
    private int cantidad;
    private String observacion;
    private boolean paraLlevar;
    private List<PedidoItemResponse> items;
}
