package com.micomunity.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    
    private Long id;
    private String contenido;
    private String usuarioNombre;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String tipo; // "mensaje", "historial", "usuario_conectado", etc.
    
    public ChatMessageDTO(String contenido, String usuarioNombre, LocalDateTime timestamp) {
        this.contenido = contenido;
        this.usuarioNombre = usuarioNombre;
        this.timestamp = timestamp;
        this.tipo = "mensaje";
    }
    
    public ChatMessageDTO(String contenido, String usuarioNombre) {
        this.contenido = contenido;
        this.usuarioNombre = usuarioNombre;
        this.timestamp = LocalDateTime.now();
        this.tipo = "mensaje";
    }
} 