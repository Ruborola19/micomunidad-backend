package com.micomunity.backend.controller;

import com.micomunity.backend.dto.ReservaRequest;
import com.micomunity.backend.dto.ReservaResponse;
import com.micomunity.backend.dto.CalendarioReservasResponse;
import com.micomunity.backend.dto.MisReservasResponse;
import com.micomunity.backend.dto.HorariosDisponiblesResponse;
import com.micomunity.backend.model.User;
import com.micomunity.backend.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Slf4j
public class ReservaController {

    private final ReservaService reservaService;

    /**
     * Crear nueva reserva (solo vecinos)
     * POST /api/reservas
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('VECINO', 'PRESIDENTE')")
    public ResponseEntity<ReservaResponse> crearReserva(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ReservaRequest request) {
        try {
            log.info("Creando reserva para zona: {}", request.getZonaComunId());
            ReservaResponse response = reservaService.crearReserva(user, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error al crear reserva: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Cancelar reserva (solo vecino propietario)
     * DELETE /api/reservas/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('VECINO', 'PRESIDENTE')")
    public ResponseEntity<Void> cancelarReserva(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        try {
            log.info("Cancelando reserva: {}", id);
            reservaService.cancelarReserva(user, id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error al cancelar reserva {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Ver reservas de una zona específica
     * GET /api/zonas/{zonaId}/reservas
     */
    @GetMapping("/zona/{zonaId}")
    public ResponseEntity<List<ReservaResponse>> obtenerReservasDeZona(
            @AuthenticationPrincipal User user,
            @PathVariable UUID zonaId) {
        try {
            log.info("Obteniendo reservas de zona: {}", zonaId);
            List<ReservaResponse> reservas = reservaService.obtenerReservasDeZona(user, zonaId);
            return ResponseEntity.ok(reservas);
        } catch (Exception e) {
            log.error("Error al obtener reservas de zona {}: {}", zonaId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Ver historial de reservas (solo presidente)
     * GET /api/reservas/historial
     */
    @GetMapping("/historial")
    @PreAuthorize("hasRole('PRESIDENTE')")
    public ResponseEntity<List<ReservaResponse>> obtenerHistorialReservas(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) UUID zonaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            log.info("Obteniendo historial de reservas. Zona: {}, Fecha inicio: {}, Fecha fin: {}", 
                    zonaId, fechaInicio, fechaFin);
            List<ReservaResponse> reservas = reservaService.obtenerHistorialReservas(user, zonaId, fechaInicio, fechaFin);
            return ResponseEntity.ok(reservas);
        } catch (Exception e) {
            log.error("Error al obtener historial de reservas: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Ver mis reservas (usuario actual)
     * GET /api/reservas/mis-reservas
     */
    @GetMapping("/mis-reservas")
    public ResponseEntity<List<MisReservasResponse>> obtenerMisReservas(
            @AuthenticationPrincipal User user) {
        try {
            log.info("Obteniendo reservas del usuario: {}", user.getEmail());
            List<MisReservasResponse> reservas = reservaService.obtenerMisReservas(user);
            return ResponseEntity.ok(reservas);
        } catch (Exception e) {
            log.error("Error al obtener reservas del usuario {}: {}", user.getEmail(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Ver calendario de una zona específica para una fecha
     * GET /api/reservas/calendario/{zonaId}
     */
    @GetMapping("/calendario/{zonaId}")
    public ResponseEntity<CalendarioReservasResponse> obtenerCalendarioZona(
            @AuthenticationPrincipal User user,
            @PathVariable UUID zonaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        try {
            log.info("Obteniendo calendario de zona {} para fecha {}", zonaId, fecha);
            CalendarioReservasResponse calendario = reservaService.obtenerCalendarioZona(user, zonaId, fecha);
            return ResponseEntity.ok(calendario);
        } catch (Exception e) {
            log.error("Error al obtener calendario de zona {}: {}", zonaId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Ver todas las reservas de la comunidad (para vista general)
     * GET /api/reservas/comunidad
     */
    @GetMapping("/comunidad")
    public ResponseEntity<List<ReservaResponse>> obtenerReservasComunidad(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            log.info("Obteniendo reservas de la comunidad del {} al {}", fechaInicio, fechaFin);
            List<ReservaResponse> reservas = reservaService.obtenerReservasComunidad(user, fechaInicio, fechaFin);
            return ResponseEntity.ok(reservas);
        } catch (Exception e) {
            log.error("Error al obtener reservas de la comunidad: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Ver horarios disponibles para formulario de reserva
     * GET /api/reservas/horarios-disponibles/{zonaId}
     */
    @GetMapping("/horarios-disponibles/{zonaId}")
    public ResponseEntity<HorariosDisponiblesResponse> obtenerHorariosDisponibles(
            @AuthenticationPrincipal User user,
            @PathVariable UUID zonaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        try {
            log.info("Obteniendo horarios disponibles para zona {} el {}", zonaId, fecha);
            HorariosDisponiblesResponse horarios = reservaService.obtenerHorariosDisponibles(user, zonaId, fecha);
            return ResponseEntity.ok(horarios);
        } catch (Exception e) {
            log.error("Error al obtener horarios disponibles: {}", e.getMessage(), e);
            throw e;
        }
    }
} 