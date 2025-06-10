package com.micomunity.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PerfilUsuarioResponse {
    private String email;
    private String rol;
    private String dni;
    private String nombreCompleto;
    private String piso;
} 