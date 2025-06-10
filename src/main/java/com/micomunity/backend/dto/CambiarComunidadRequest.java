package com.micomunity.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CambiarComunidadRequest {
    @NotBlank(message = "El código de la nueva comunidad es obligatorio")
    private String codigoNuevaComunidad;
} 