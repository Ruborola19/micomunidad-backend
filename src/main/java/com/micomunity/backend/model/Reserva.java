package com.micomunity.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_comun_id", nullable = false)
    @JsonBackReference("zona-reservas")
    private ZonaComun zonaComun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonBackReference("user-reservas")
    private User usuario;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(nullable = false, name = "hora_fin")
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado;

    @Column(nullable = false, name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    // Constructor de conveniencia
    public Reserva(ZonaComun zonaComun, User usuario, LocalDate fecha, 
                   LocalTime horaInicio, LocalTime horaFin) {
        this.zonaComun = zonaComun;
        this.usuario = usuario;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.estado = EstadoReserva.ACTIVA;
        this.fechaCreacion = LocalDateTime.now();
    }

    public void cancelar() {
        this.estado = EstadoReserva.CANCELADA;
        this.fechaCancelacion = LocalDateTime.now();
    }
} 