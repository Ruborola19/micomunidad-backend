package com.micomunity.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CederPresidenciaRequest {
    @NotNull(message = "El ID del nuevo presidente es obligatorio")
    private Long idNuevoPresidente;
} 