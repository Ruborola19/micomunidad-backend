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
public class HorariosDisponiblesResponse {
    
    private UUID zonaComunId;
    private String zonaComunNombre;
    private LocalDate fecha;
    private List<HorarioDisponible> horariosDisponibles;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HorarioDisponible {
        private String horaInicio;
        private String horaFin;
        private boolean disponible;
        private String motivoNoDisponible;
    }
} 