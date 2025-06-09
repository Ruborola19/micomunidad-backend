package com.micomunity.backend.controller;

import com.micomunity.backend.dto.ZonaComunRequest;
import com.micomunity.backend.dto.ZonaComunResponse;
import com.micomunity.backend.model.User;
import com.micomunity.backend.service.ZonaComunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/zonas")
@RequiredArgsConstructor
@Slf4j
public class ZonaComunController {

    private final ZonaComunService zonaComunService;

    /**
     * Crear nueva zona común (solo presidente)
     * POST /api/zonas
     */
    @PostMapping
    @PreAuthorize("hasRole('PRESIDENTE')")
    public ResponseEntity<ZonaComunResponse> crearZonaComun(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ZonaComunRequest request) {
        try {
            log.info("Creando zona común: {}", request.getNombre());
            ZonaComunResponse response = zonaComunService.crearZonaComun(user, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error al crear zona común: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Eliminar zona común (solo presidente)
     * DELETE /api/zonas/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PRESIDENTE')")
    public ResponseEntity<Void> eliminarZonaComun(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        try {
            log.info("Eliminando zona común: {}", id);
            zonaComunService.eliminarZonaComun(user, id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error al eliminar zona común {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Listar zonas comunes por comunidad (todos los roles)
     * GET /api/zonas
     */
    @GetMapping
    public ResponseEntity<List<ZonaComunResponse>> obtenerZonasComunes(
            @AuthenticationPrincipal User user) {
        try {
            log.info("Obteniendo zonas comunes para usuario: {}", user.getEmail());
            List<ZonaComunResponse> zonas = zonaComunService.obtenerZonasComunes(user);
            return ResponseEntity.ok(zonas);
        } catch (Exception e) {
            log.error("Error al obtener zonas comunes: {}", e.getMessage(), e);
            throw e;
        }
    }
} 