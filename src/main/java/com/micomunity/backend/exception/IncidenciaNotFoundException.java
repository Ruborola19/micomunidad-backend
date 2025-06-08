package com.micomunity.backend.exception;
 
public class IncidenciaNotFoundException extends ResourceNotFoundException {
    public IncidenciaNotFoundException(Long id) {
        super("Incidencia no encontrada con ID: " + id);
    }
} 