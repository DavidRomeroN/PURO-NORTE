package com.anticucheria.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SustitucionDTO {

    @NotNull
    private Long comboSlotId;

    @NotNull
    private Long productoBaseNuevoId;
}
