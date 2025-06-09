package com.micomunity.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZonaComunRequest {
    
    @NotBlank(message = "El nombre de la zona común no puede estar vacío")
    private String nombre;
} 