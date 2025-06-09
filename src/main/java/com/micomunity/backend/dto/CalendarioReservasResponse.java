package com.micomunity.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarioReservasResponse {
    
    private UUID zonaComunId;
    private String zonaComunNombre;
    private LocalDate fecha;
    private List<ReservaCalendarioDTO> reservas;
    private List<String> horasDisponibles;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservaCalendarioDTO {
        private UUID id;
        private String horaInicio;
        private String horaFin;
        private String usuarioNombre;
        private boolean esPropia;
        private boolean puedeCancelar;
    }
} 