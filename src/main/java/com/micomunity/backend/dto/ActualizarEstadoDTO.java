package com.micomunity.backend.dto;

import com.micomunity.backend.model.EstadoIncidencia;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
 
@Data
public class ActualizarEstadoDTO {
    @NotNull(message = "El estado no puede ser nulo")
    private EstadoIncidencia estado;
} 