package com.micomunity.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Voto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String opcionSeleccionada;

    @ManyToOne
    @JoinColumn(name = "votante_id")
    private User votante;

    @ManyToOne
    @JoinColumn(name = "votacion_id")
    private Votacion votacion;

    @Column(nullable = false, unique = true)
    private String claveUnica; // Ej: usuarioId_votacionId, para evitar votos duplicados
}
