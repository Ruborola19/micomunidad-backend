package com.micomunity.backend.controller;

import com.micomunity.backend.dto.ChatMessageDTO;
import com.micomunity.backend.model.ChatMessage;
import com.micomunity.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    /**
     * Obtener historial de mensajes (endpoint REST alternativo)
     * GET /api/chat/historial?limite=50
     */
    @GetMapping("/historial")
    public ResponseEntity<List<ChatMessageDTO>> obtenerHistorial(
            @RequestParam(defaultValue = "50") int limite) {
        
        log.info("Solicitando historial de {} mensajes via REST", limite);
        
        List<ChatMessage> mensajes = chatService.obtenerHistorial(limite);
        
        List<ChatMessageDTO> mensajesDTO = mensajes.stream()
                .map(mensaje -> new ChatMessageDTO(
                        mensaje.getId(),
                        mensaje.getContenido(),
                        mensaje.getUsuarioNombre(),
                        mensaje.getTimestamp(),
                        "historial"
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(mensajesDTO);
    }

    /**
     * Obtener estadísticas del chat
     * GET /api/chat/estadisticas
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PRESIDENTE')")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        log.info("Solicitando estadísticas del chat");
        
        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("totalMensajes", chatService.contarTotalMensajes());
        estadisticas.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(estadisticas);
    }

    /**
     * Eliminar un mensaje (solo administradores)
     * DELETE /api/chat/mensaje/{id}
     */
    @DeleteMapping("/mensaje/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, String>> eliminarMensaje(@PathVariable Long id) {
        log.info("Solicitando eliminación del mensaje ID: {}", id);
        
        boolean eliminado = chatService.eliminarMensaje(id);
        
        Map<String, String> response = new HashMap<>();
        if (eliminado) {
            response.put("mensaje", "Mensaje eliminado exitosamente");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } else {
            response.put("mensaje", "No se pudo eliminar el mensaje");
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Endpoint de prueba para verificar que el chat está funcionando
     * GET /api/chat/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> obtenerStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("chatActivo", true);
        status.put("endpoint", "ws://localhost:8080/ws/chat");
        status.put("corsHabilitado", true);
        status.put("historialDisponible", true);
        status.put("totalMensajes", chatService.contarTotalMensajes());
        
        return ResponseEntity.ok(status);
    }
} 