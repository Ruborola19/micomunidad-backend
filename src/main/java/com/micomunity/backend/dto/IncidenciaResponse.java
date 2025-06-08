package com.micomunity.backend.dto;

import com.micomunity.backend.model.EstadoIncidencia;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidenciaResponse {
    private Long id;
    private String titulo;
    private String descripcion;
    private String ubicacion;
    private String fechaCreacion;
    private Long autorId;
    private String autorNombre;
    private String codigoComunidad;
    private EstadoIncidencia estado;
    private String imagenUrl;
} 