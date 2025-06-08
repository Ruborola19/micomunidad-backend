package com.micomunity.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class VotacionResponseDTO {

    private Long id;
    private String titulo;
    private String descripcion;

    private String opcion1;
    private String opcion2;
    private String opcion3;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaFinal;

    private boolean finalizada;
    private boolean yaVotado;

    private Map<String, Long> resultados; // solo si corresponde
}
