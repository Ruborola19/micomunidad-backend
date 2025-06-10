package com.micomunity.backend.service;

import com.micomunity.backend.dto.PerfilUsuarioResponse;
import com.micomunity.backend.dto.CambiarPasswordRequest;
import com.micomunity.backend.model.User;
import com.micomunity.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PerfilUsuarioService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PerfilUsuarioResponse obtenerPerfil(User user) {
        log.info("Usuario {} obteniendo su perfil", user.getEmail());
        
        return new PerfilUsuarioResponse(
                user.getEmail(),
                user.getRole().name(),
                user.getDni(),
                user.getFullName(),
                user.getFloor()
        );
    }

    @Transactional
    public void cambiarPassword(User user, CambiarPasswordRequest request) {
        log.info("Usuario {} cambiando contraseña", user.getEmail());
        
        // Validar que las nuevas contraseñas coinciden
        if (!request.getNuevaPassword().equals(request.getRepetirNuevaPassword())) {
            throw new RuntimeException("Las nuevas contraseñas no coinciden");
        }

        // Validar que la contraseña actual es correcta
        if (!passwordEncoder.matches(request.getPasswordActual(), user.getPassword())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }

        // Validar que la nueva contraseña es diferente a la actual
        if (passwordEncoder.matches(request.getNuevaPassword(), user.getPassword())) {
            throw new RuntimeException("La nueva contraseña debe ser diferente a la actual");
        }

        // Encriptar y guardar la nueva contraseña
        user.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
        userRepository.save(user);

        log.info("Contraseña cambiada exitosamente para el usuario {}", user.getEmail());
    }
} 