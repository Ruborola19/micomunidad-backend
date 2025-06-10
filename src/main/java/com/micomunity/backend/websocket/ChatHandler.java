package com.micomunity.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.micomunity.backend.dto.ChatMessageDTO;
import com.micomunity.backend.model.ChatMessage;
import com.micomunity.backend.model.Community;
import com.micomunity.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatHandler implements WebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    
    // Mapa para guardar todas las sesiones activas
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // Mapa para asociar sesiones con comunidades (si implementas multi-community)
    private final Map<String, Long> sessionToCommunity = new ConcurrentHashMap<>();
    
    // Configuración: número de mensajes del historial
    private static final int HISTORY_LIMIT = 50;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        
        log.info("Nueva conexión WebSocket establecida: {}", sessionId);
        
        // Obtener IP del cliente
        String clientIp = getClientIpAddress(session);
        log.debug("IP del cliente: {}", clientIp);
        
        // Enviar historial de mensajes al nuevo usuario
        enviarHistorial(session);
        
        // Notificar a otros usuarios que alguien se conectó (opcional)
        ChatMessageDTO connectionMessage = new ChatMessageDTO();
        connectionMessage.setTipo("usuario_conectado");
        connectionMessage.setContenido("Un usuario se ha conectado al chat");
        connectionMessage.setUsuarioNombre("Sistema");
        
        broadcastMessage(connectionMessage, sessionId); // No enviar al usuario que se acaba de conectar
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload().toString();
        
        log.info("Mensaje recibido de {}: {}", sessionId, payload);
        
        try {
            // El frontend puede enviar texto plano o JSON
            ChatMessageDTO messageDTO;
            
            if (payload.startsWith("{")) {
                // Es JSON
                messageDTO = objectMapper.readValue(payload, ChatMessageDTO.class);
            } else {
                // Es texto plano
                messageDTO = new ChatMessageDTO();
                messageDTO.setContenido(payload);
                messageDTO.setUsuarioNombre("Usuario"); // Nombre por defecto
                messageDTO.setTipo("mensaje");
            }
            
            // Obtener IP del cliente
            String clientIp = getClientIpAddress(session);
            
            // Guardar mensaje en la base de datos
            ChatMessage savedMessage = chatService.guardarMensaje(
                messageDTO.getContenido(), 
                messageDTO.getUsuarioNombre(), 
                clientIp,
                null // Por ahora sin comunidad específica, se puede implementar después
            );
            
            // Crear DTO para broadcast
            ChatMessageDTO broadcastDTO = new ChatMessageDTO(
                savedMessage.getId(),
                savedMessage.getContenido(),
                savedMessage.getUsuarioNombre(),
                savedMessage.getTimestamp(),
                "mensaje"
            );
            
            // Retransmitir a todos los usuarios conectados
            broadcastMessage(broadcastDTO, null); // null = enviar a todos
            
        } catch (Exception e) {
            log.error("Error al procesar mensaje: {}", e.getMessage(), e);
            // Enviar mensaje de error al cliente
            enviarMensajeError(session, "Error al procesar el mensaje");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Error de transporte WebSocket en sesión {}: {}", session.getId(), exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        sessionToCommunity.remove(sessionId);
        
        log.info("Conexión WebSocket cerrada: {} - Status: {}", sessionId, closeStatus);
        
        // Notificar a otros usuarios que alguien se desconectó (opcional)
        ChatMessageDTO disconnectionMessage = new ChatMessageDTO();
        disconnectionMessage.setTipo("usuario_desconectado");
        disconnectionMessage.setContenido("Un usuario se ha desconectado del chat");
        disconnectionMessage.setUsuarioNombre("Sistema");
        
        broadcastMessage(disconnectionMessage, null);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    /**
     * Envía el historial de mensajes a una sesión específica
     */
    private void enviarHistorial(WebSocketSession session) {
        try {
            List<ChatMessage> historial = chatService.obtenerHistorial(HISTORY_LIMIT);
            
            log.debug("Enviando {} mensajes del historial a {}", historial.size(), session.getId());
            
            for (ChatMessage mensaje : historial) {
                ChatMessageDTO dto = new ChatMessageDTO(
                    mensaje.getId(),
                    mensaje.getContenido(),
                    mensaje.getUsuarioNombre(),
                    mensaje.getTimestamp(),
                    "historial"
                );
                
                enviarMensaje(session, dto);
            }
            
        } catch (Exception e) {
            log.error("Error al enviar historial: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Retransmite un mensaje a todos los usuarios conectados
     */
    private void broadcastMessage(ChatMessageDTO message, String excludeSessionId) {
        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Error al serializar mensaje: {}", e.getMessage());
            return;
        }
        
        sessions.forEach((sessionId, session) -> {
            if (!sessionId.equals(excludeSessionId) && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(messageJson));
                } catch (IOException e) {
                    log.error("Error al enviar mensaje a sesión {}: {}", sessionId, e.getMessage());
                    // Remover sesión problemática
                    sessions.remove(sessionId);
                }
            }
        });
    }
    
    /**
     * Envía un mensaje a una sesión específica
     */
    private void enviarMensaje(WebSocketSession session, ChatMessageDTO message) {
        if (session.isOpen()) {
            try {
                String messageJson = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(messageJson));
            } catch (Exception e) {
                log.error("Error al enviar mensaje a sesión {}: {}", session.getId(), e.getMessage());
            }
        }
    }
    
    /**
     * Envía un mensaje de error a una sesión específica
     */
    private void enviarMensajeError(WebSocketSession session, String errorMessage) {
        ChatMessageDTO errorDTO = new ChatMessageDTO();
        errorDTO.setTipo("error");
        errorDTO.setContenido(errorMessage);
        errorDTO.setUsuarioNombre("Sistema");
        
        enviarMensaje(session, errorDTO);
    }
    
    /**
     * Obtiene la IP del cliente desde la sesión WebSocket
     */
    private String getClientIpAddress(WebSocketSession session) {
        try {
            InetSocketAddress remoteAddress = session.getRemoteAddress();
            return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
        } catch (Exception e) {
            log.debug("No se pudo obtener la IP del cliente: {}", e.getMessage());
            return "unknown";
        }
    }
} 