package com.micomunity.backend.service;

import com.micomunity.backend.dto.LoginDTO;
import com.micomunity.backend.dto.PresidentRegistrationDTO;
import com.micomunity.backend.dto.UserRegistrationDTO;
import com.micomunity.backend.model.Community;
import com.micomunity.backend.model.Role;
import com.micomunity.backend.model.User;
import com.micomunity.backend.repository.CommunityRepository;
import com.micomunity.backend.repository.UserRepository;
import com.micomunity.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public Map<String, Object> registerPresident(PresidentRegistrationDTO request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        if (userRepository.existsByDni(request.getDni())) {
            throw new IllegalArgumentException("El DNI ya está registrado");
        }

        String communityCode = request.getCommunityCode();
        Community community = new Community();
        community.setName(request.getCommunityName());
        community.setAddress(request.getAddress());
        community.setPostalCode(request.getPostalCode());
        community.setCommunityCode(communityCode);

        // Guardar comunidad primero
        community = communityRepository.save(community);

        // Crear presidente ya con comunidad persistida
        User president = new User();
        president.setDni(request.getDni());
        president.setFullName(request.getFullName());
        president.setFloor(request.getFloor());
        president.setEmail(request.getEmail());
        president.setPassword(passwordEncoder.encode(request.getPassword()));
        president.setRole(Role.PRESIDENTE);
        president.setCommunity(community);

        president = userRepository.save(president);

        // Asociar presidente a la comunidad y actualizar
        community.setPresident(president);
        communityRepository.save(community);

        String token = jwtService.generateToken(president);

        Map<String, Object> userData = buildUserData(president);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", userData);

        return response;
    }

    @Transactional
    public Map<String, Object> registerUser(UserRegistrationDTO request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        if (userRepository.existsByDni(request.getDni())) {
            throw new IllegalArgumentException("El DNI ya está registrado");
        }

        Community community = communityRepository.findByCommunityCode(request.getCommunityCode())
                .orElseThrow(() -> new IllegalArgumentException("Código de comunidad no válido"));

        User user = new User();
        user.setDni(request.getDni());
        user.setFullName(request.getFullName());
        user.setFloor(request.getFloor());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setCommunity(community);

        user = userRepository.save(user);

        String token = jwtService.generateToken(user);

        Map<String, Object> userData = buildUserData(user);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", userData);

        return response;
    }

    public Map<String, Object> login(LoginDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmailWithCommunity(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String token = jwtService.generateToken(user);

        Map<String, Object> userData = buildUserData(user);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", userData);

        return response;
    }

    public Map<String, Object> getUserInfoFromToken(String token) {
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmailWithCommunity(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return buildUserData(user);
    }

    public boolean validateToken(String token) {
        try {
            String email = jwtService.extractUsername(token);
            User user = userRepository.findByEmailWithCommunity(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            return jwtService.isTokenValid(token, user);
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> buildUserData(User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("name", user.getFullName());
        userData.put("role", user.getRole().name());
        userData.put("communityId", user.getCommunity().getCommunityCode());
        userData.put("communityCode", user.getCommunity().getCommunityCode());
        userData.put("communityName", user.getCommunity().getName());
        return userData;
    }

    private String generateCommunityCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (communityRepository.existsByCommunityCode(code));

        return code;
    }
}
