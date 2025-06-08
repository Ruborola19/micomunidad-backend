package com.micomunity.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Votacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private String opcion1;

    @Column(nullable = false)
    private String opcion2;

    @Column(nullable = false)
    private String opcion3;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaFinal;

    @Column(nullable = false)
    private int duracionHoras;

    @ManyToOne
    @JoinColumn(name = "creador_id")
    private User creador;

    @ManyToOne
    @JoinColumn(name = "community_id")
    private Community comunidad;

    @OneToMany(mappedBy = "votacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Voto> votos;
}
