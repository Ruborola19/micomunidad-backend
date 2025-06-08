package com.micomunity.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.micomunity.backend.model.TipoDocumento;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoResponse {
    private Long id;
    private String comentario;
    private TipoDocumento tipo;
    private String autorNombre;
    private String autorEmail;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaPublicacion;
    
    private List<String> urlsArchivos;
    private boolean puedeEliminar;
} 