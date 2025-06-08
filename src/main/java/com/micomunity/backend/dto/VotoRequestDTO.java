package com.micomunity.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VotoRequestDTO {

    @NotNull
    private Long votacionId;

    @NotBlank
    private String opcionSeleccionada;
}
