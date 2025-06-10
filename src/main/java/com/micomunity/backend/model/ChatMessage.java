package com.micomunity.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(nullable = false, name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "usuario_nombre")
    private String usuarioNombre;

    @Column(name = "ip_origen")
    private String ipOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    @JsonBackReference("community-chat")
    private Community community;

    public ChatMessage(String contenido, String usuarioNombre, String ipOrigen, Community community) {
        this.contenido = contenido;
        this.usuarioNombre = usuarioNombre;
        this.ipOrigen = ipOrigen;
        this.community = community;
        this.timestamp = LocalDateTime.now();
    }
} 