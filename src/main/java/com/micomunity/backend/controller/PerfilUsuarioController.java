package com.micomunity.backend.controller;

import com.micomunity.backend.dto.PerfilUsuarioResponse;
import com.micomunity.backend.dto.CambiarPasswordRequest;
import com.micomunity.backend.model.User;
import com.micomunity.backend.service.PerfilUsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Slf4j
public class PerfilUsuarioController {

    private final PerfilUsuarioService perfilUsuarioService;

    /**
     * Obtener perfil del usuario actual
     * GET /api/usuarios/perfil
     */
    @GetMapping("/perfil")
    @PreAuthorize("hasAnyRole('PRESIDENTE', 'VECINO')")
    public ResponseEntity<PerfilUsuarioResponse> obtenerPerfil(
            @AuthenticationPrincipal User user) {
        try {
            log.info("Obteniendo perfil para usuario: {}", user.getEmail());
            PerfilUsuarioResponse response = perfilUsuarioService.obtenerPerfil(user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al obtener perfil del usuario: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Cambiar contraseña del usuario
     * PUT /api/usuarios/cambiar-password
     */
    @PutMapping("/cambiar-password")
    @PreAuthorize("hasAnyRole('PRESIDENTE', 'VECINO')")
    public ResponseEntity<Void> cambiarPassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CambiarPasswordRequest request) {
        try {
            log.info("Usuario {} cambiando contraseña", user.getEmail());
            perfilUsuarioService.cambiarPassword(user, request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error al cambiar contraseña: {}", e.getMessage(), e);
            throw e;
        }
    }
} 