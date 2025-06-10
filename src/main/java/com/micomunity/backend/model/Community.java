package com.micomunity.backend.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "communities")
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(name = "community_code", nullable = false, unique = true)
    private String communityCode;

    @OneToMany(mappedBy = "community", fetch = FetchType.LAZY)
    @JsonManagedReference("user-community")
    private Set<User> users = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "president_id")
    @JsonManagedReference("president-community")
    private User president;

    @OneToMany(mappedBy = "community", fetch = FetchType.LAZY)
    @JsonManagedReference("community-incidencias")
    private Set<Incidencia> incidencias = new HashSet<>();

    @OneToMany(mappedBy = "community", fetch = FetchType.LAZY)
    @JsonManagedReference("community-zonas")
    private Set<ZonaComun> zonasComunes = new HashSet<>();

    @OneToMany(mappedBy = "community", fetch = FetchType.LAZY)
    @JsonManagedReference("community-chat")
    private Set<ChatMessage> chatMessages = new HashSet<>();

    public String getCommunityCode() {
        return this.communityCode;
    }

    public void setCommunityCode(String communityCode) {
        this.communityCode = communityCode;
    }
}
