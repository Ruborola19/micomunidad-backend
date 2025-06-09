package com.micomunity.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "zonas_comunes", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"nombre", "community_id"}))
public class ZonaComun {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    @JsonBackReference("community-zonas")
    private Community community;

    @OneToMany(mappedBy = "zonaComun", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonManagedReference("zona-reservas")
    private Set<Reserva> reservas = new HashSet<>();

    // Constructor de conveniencia
    public ZonaComun(String nombre, Community community) {
        this.nombre = nombre;
        this.community = community;
    }
} 