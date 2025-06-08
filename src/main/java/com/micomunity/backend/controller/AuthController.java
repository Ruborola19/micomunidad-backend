package com.micomunity.backend.controller;

import com.micomunity.backend.dto.LoginDTO;
import com.micomunity.backend.dto.PresidentRegistrationDTO;
import com.micomunity.backend.dto.UserRegistrationDTO;
import com.micomunity.backend.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register/president")
    public ResponseEntity<Map<String, Object>> registerPresident(
            @Valid @RequestBody PresidentRegistrationDTO request
    ) {
        log.debug("Recibida petición de registro de presidente: {}", request.getEmail());
        try {
            Map<String, Object> response = authenticationService.registerPresident(request);
            log.debug("Presidente registrado correctamente: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al registrar presidente: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerUser(
            @Valid @RequestBody UserRegistrationDTO request
    ) {
        log.debug("Recibida petición de registro de usuario: {}", request.getEmail());
        try {
            Map<String, Object> response = authenticationService.registerUser(request);
            log.debug("Usuario registrado correctamente: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al registrar usuario: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginDTO request
    ) {
        log.debug("Recibida petición de login: {}", request.getEmail());
        try {
            Map<String, Object> response = authenticationService.login(request);
            log.debug("Login correcto para: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error en login: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getAuthenticatedUserInfo(
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token JWT no proporcionado"));
        }

        String token = authHeader.substring(7); // Quitar "Bearer "
        Map<String, Object> response = authenticationService.getUserInfoFromToken(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Token no válido"));
        }

        String token = authHeader.substring(7);
        boolean isValid = authenticationService.validateToken(token);

        if (isValid) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "Token expirado o inválido"));
        }
    }
}
