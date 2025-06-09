package com.micomunity.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZonaComunResponse {
    
    private UUID id;
    private String nombre;
    private String communityCode;
    private Long communityId;
    private boolean puedeEliminar; // Para indicar si el usuario actual puede eliminarla
} 