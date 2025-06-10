package com.micomunity.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MiComunidadResponse {
    private String nombre;
    private String codigo;
    private String presidente;
    private List<VecinoInfo> vecinos;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VecinoInfo {
        private Long id;
        private String nombre;
        private String piso;
    }
} 