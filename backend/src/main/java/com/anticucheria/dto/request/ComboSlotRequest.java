package com.anticucheria.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComboSlotRequest {

    @NotNull
    private Long productoBaseDefaultId;

    @NotNull
    @Positive
    private Integer orden;

    @NotNull
    private Boolean esCortesia;

    @NotNull
    private Boolean esSustituible;
}
