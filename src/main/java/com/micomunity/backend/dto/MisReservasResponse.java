package com.micomunity.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.micomunity.backend.model.EstadoReserva;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MisReservasResponse {
    
    private UUID id;
    private UUID zonaComunId;
    private String zonaComunNombre;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;
    
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaInicio;
    
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaFin;
    
    private EstadoReserva estado;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaCreacion;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaCancelacion;
    
    private boolean puedeCancelar;
    private boolean yaComenzo;
    private boolean yaTermino;
    private long horasHastaInicio;
} 