package com.micomunity.backend.controller;

import com.micomunity.backend.dto.MiComunidadResponse;
import com.micomunity.backend.dto.CambiarComunidadRequest;
import com.micomunity.backend.dto.CederPresidenciaRequest;
import com.micomunity.backend.model.User;
import com.micomunity.backend.model.Community;
import com.micomunity.backend.service.ComunidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comunidad")
@RequiredArgsConstructor
@Slf4j
public class ComunidadController {

    private final ComunidadService comunidadService;

    /**
     * Obtener información de mi comunidad
     * GET /api/comunidad/mia
     */
    @GetMapping("/mia")
    @PreAuthorize("hasAnyRole('PRESIDENTE', 'VECINO')")
    public ResponseEntity<MiComunidadResponse> obtenerMiComunidad(
            @AuthenticationPrincipal User user) {
        try {
            log.info("Obteniendo información de comunidad para usuario: {}", user.getEmail());
            MiComunidadResponse response = comunidadService.obtenerMiComunidad(user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al obtener información de la comunidad: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Cambiar de comunidad
     * PUT /api/comunidad/cambiar
     */
    @PutMapping("/cambiar")
    @PreAuthorize("hasAnyRole('PRESIDENTE', 'VECINO')")
    public ResponseEntity<Void> cambiarComunidad(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CambiarComunidadRequest request) {
        try {
            log.info("Usuario {} cambiando de comunidad", user.getEmail());
            comunidadService.cambiarComunidad(user, request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error al cambiar de comunidad: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Ceder presidencia a otro vecino
     * PUT /api/comunidad/ceder-presidencia
     */
    @PutMapping("/ceder-presidencia")
    @PreAuthorize("hasRole('PRESIDENTE')")
    public ResponseEntity<Void> cederPresidencia(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CederPresidenciaRequest request) {
        try {
            log.info("Presidente {} cediendo presidencia", user.getEmail());
            comunidadService.cederPresidencia(user, request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error al ceder presidencia: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Endpoint temporal de debugging para verificar usuarios en la comunidad
     * GET /api/comunidad/debug/usuarios
     */
    @GetMapping("/debug/usuarios")
    @PreAuthorize("hasAnyRole('PRESIDENTE', 'ADMINISTRADOR')")
    public ResponseEntity<List<Map<String, Object>>> debugUsuarios(
            @AuthenticationPrincipal User user) {
        try {
            log.info("Debug: Usuario {} consultando usuarios de su comunidad", user.getEmail());
            
            Community community = user.getCommunity();
            if (community == null) {
                throw new RuntimeException("El usuario no pertenece a ninguna comunidad");
            }

            // Obtener usuarios directamente del repositorio
            List<User> usuarios = comunidadService.obtenerUsuariosParaDebug(community);
            
            List<Map<String, Object>> usuariosInfo = usuarios.stream()
                    .map(u -> {
                        Map<String, Object> info = new HashMap<>();
                        info.put("id", u.getId());
                        info.put("nombre", u.getFullName());
                        info.put("email", u.getEmail());
                        info.put("rol", u.getRole().name());
                        info.put("piso", u.getFloor());
                        info.put("communityId", u.getCommunity().getId());
                        return info;
                    })
                    .collect(Collectors.toList());

            log.info("Debug: Encontrados {} usuarios en total", usuariosInfo.size());
            return ResponseEntity.ok(usuariosInfo);
            
        } catch (Exception e) {
            log.error("Error en debug de usuarios: {}", e.getMessage(), e);
            throw e;
        }
    }
} 