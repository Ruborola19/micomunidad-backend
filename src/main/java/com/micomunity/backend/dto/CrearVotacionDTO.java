package com.micomunity.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearVotacionDTO {

    @NotBlank
    private String titulo;

    @NotBlank
    private String descripcion;

    @NotBlank
    private String opcion1;

    @NotBlank
    private String opcion2;

    @NotBlank
    private String opcion3;

    @NotNull
    private Integer duracionHoras;
}
