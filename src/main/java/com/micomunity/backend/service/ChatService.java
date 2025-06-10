package com.micomunity.backend.service;

import com.micomunity.backend.model.ChatMessage;
import com.micomunity.backend.model.Community;
import com.micomunity.backend.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;

    /**
     * Guarda un nuevo mensaje en la base de datos
     */
    @Transactional
    public ChatMessage guardarMensaje(String contenido, String usuarioNombre, String ipOrigen, Community community) {
        log.debug("Guardando mensaje: {} - Usuario: {} - IP: {}", contenido, usuarioNombre, ipOrigen);
        
        ChatMessage mensaje = new ChatMessage();
        mensaje.setContenido(contenido);
        mensaje.setUsuarioNombre(usuarioNombre != null ? usuarioNombre : "Usuario");
        mensaje.setIpOrigen(ipOrigen);
        mensaje.setTimestamp(LocalDateTime.now());
        mensaje.setCommunity(community); // null por ahora para chat global
        
        ChatMessage savedMessage = chatMessageRepository.save(mensaje);
        log.info("Mensaje guardado con ID: {}", savedMessage.getId());
        
        return savedMessage;
    }

    /**
     * Obtiene el historial de mensajes (últimos N mensajes)
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> obtenerHistorial(int limite) {
        log.debug("Obteniendo historial de {} mensajes", limite);
        
        try {
            // Obtener todos los mensajes ordenados por timestamp descendente (más recientes primero)
            PageRequest pageRequest = PageRequest.of(0, limite, 
                org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.DESC, "timestamp"));
            
            List<ChatMessage> mensajes = chatMessageRepository.findAll(pageRequest).getContent();
            
            // Crear una lista mutable e invertir el orden para mostrar cronológicamente (más antiguos primero)
            List<ChatMessage> mensajesMutable = new java.util.ArrayList<>(mensajes);
            Collections.reverse(mensajesMutable);
            mensajes = mensajesMutable;
            
            log.info("Historial obtenido: {} mensajes", mensajes.size());
            
            // Log de debug para ver qué mensajes se encontraron
            for (ChatMessage msg : mensajes) {
                log.debug("Mensaje ID: {}, Usuario: {}, Contenido: {}, Timestamp: {}", 
                    msg.getId(), msg.getUsuarioNombre(), msg.getContenido(), msg.getTimestamp());
            }
            
            return mensajes;
            
        } catch (Exception e) {
            log.error("Error al obtener historial: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Obtiene el historial de mensajes de una comunidad específica
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> obtenerHistorialPorComunidad(Community community, int limite) {
        log.debug("Obteniendo historial de {} mensajes para comunidad: {}", limite, community.getName());
        
        try {
            PageRequest pageRequest = PageRequest.of(0, limite);
            List<ChatMessage> mensajes = chatMessageRepository.findLatestMessagesByCommunityForHistory(community, pageRequest);
            Collections.reverse(mensajes); // Para mostrar cronológicamente
            
            log.debug("Historial obtenido: {} mensajes para comunidad {}", mensajes.size(), community.getName());
            return mensajes;
            
        } catch (Exception e) {
            log.error("Error al obtener historial por comunidad: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Obtiene todos los mensajes de una comunidad
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> obtenerTodosMensajesPorComunidad(Community community) {
        log.debug("Obteniendo todos los mensajes para comunidad: {}", community.getName());
        
        try {
            List<ChatMessage> mensajes = chatMessageRepository.findAllMessagesByCommunityOrderByTimestamp(community);
            log.debug("Total mensajes obtenidos: {} para comunidad {}", mensajes.size(), community.getName());
            return mensajes;
            
        } catch (Exception e) {
            log.error("Error al obtener todos los mensajes por comunidad: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Elimina un mensaje (solo para administradores)
     */
    @Transactional
    public boolean eliminarMensaje(Long messageId) {
        log.info("Intentando eliminar mensaje ID: {}", messageId);
        
        try {
            if (chatMessageRepository.existsById(messageId)) {
                chatMessageRepository.deleteById(messageId);
                log.info("Mensaje {} eliminado exitosamente", messageId);
                return true;
            } else {
                log.warn("Mensaje {} no encontrado para eliminar", messageId);
                return false;
            }
        } catch (Exception e) {
            log.error("Error al eliminar mensaje {}: {}", messageId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Obtiene estadísticas del chat
     */
    @Transactional(readOnly = true)
    public long contarTotalMensajes() {
        return chatMessageRepository.count();
    }
} 