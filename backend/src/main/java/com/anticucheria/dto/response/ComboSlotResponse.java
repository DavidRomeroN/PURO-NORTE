package com.anticucheria.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComboSlotResponse {

    private Long id;
    private Integer orden;
    private ProductoResponse productoBaseDefault;
    private Boolean esCortesia;
    private Boolean esSustituible;
}
