package com.micomunity.backend.config;

import com.micomunity.backend.websocket.ChatHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatHandler chatHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("Configurando WebSocket handlers");
        
        registry.addHandler(chatHandler, "/ws/chat")
                .setAllowedOrigins("*") // Permitir todas las conexiones CORS
                .withSockJS(); // Opcional: habilitar SockJS como fallback
        
        // También registrar sin SockJS para WebSocket puro
        registry.addHandler(chatHandler, "/ws/chat")
                .setAllowedOrigins("*");
        
        log.info("WebSocket handler registrado en: /ws/chat");
    }
} 