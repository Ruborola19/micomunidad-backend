package com.micomunity.backend.service;

import com.micomunity.backend.dto.MiComunidadResponse;
import com.micomunity.backend.dto.CambiarComunidadRequest;
import com.micomunity.backend.dto.CederPresidenciaRequest;
import com.micomunity.backend.model.Community;
import com.micomunity.backend.model.Role;
import com.micomunity.backend.model.User;
import com.micomunity.backend.repository.CommunityRepository;
import com.micomunity.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ComunidadService {

    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MiComunidadResponse obtenerMiComunidad(User user) {
        log.info("Usuario {} obteniendo información de su comunidad", user.getEmail());
        
        Community community = user.getCommunity();
        if (community == null) {
            throw new RuntimeException("El usuario no pertenece a ninguna comunidad");
        }

        // Refrescar la entidad community desde la base de datos para evitar problemas de cache
        community = communityRepository.findById(community.getId())
                .orElseThrow(() -> new RuntimeException("Comunidad no encontrada"));

        // Obtener presidente
        User presidente = community.getPresident();
        String nombrePresidente = presidente != null ? presidente.getFullName() : "No asignado";
        log.debug("Presidente actual: {}", nombrePresidente);

        // Obtener lista de vecinos - forzar recarga desde BD
        List<User> vecinos = userRepository.findByCommunityOrderByFullName(community);
        log.debug("Total usuarios en la comunidad: {}", vecinos.size());
        
        // Log detallado de cada usuario para depuración
        for (User vecino : vecinos) {
            log.debug("Usuario: {} - Rol: {} - ID: {}", 
                    vecino.getFullName(), vecino.getRole(), vecino.getId());
        }
        
        List<MiComunidadResponse.VecinoInfo> vecinosInfo = vecinos.stream()
                .map(vecino -> new MiComunidadResponse.VecinoInfo(
                        vecino.getId(),
                        vecino.getFullName(),
                        vecino.getFloor()
                ))
                .collect(Collectors.toList());

        log.info("Encontrados {} vecinos en la comunidad {}", vecinosInfo.size(), community.getName());

        return new MiComunidadResponse(
                community.getName(),
                community.getCommunityCode(),
                nombrePresidente,
                vecinosInfo
        );
    }

    @Transactional
    public void cambiarComunidad(User user, CambiarComunidadRequest request) {
        log.info("Usuario {} cambiando a comunidad {}", user.getEmail(), request.getCodigoNuevaComunidad());
        
        // Verificar que el código de la nueva comunidad existe
        Community nuevaComunidad = communityRepository.findByCommunityCode(request.getCodigoNuevaComunidad())
                .orElseThrow(() -> new RuntimeException("No existe una comunidad con el código proporcionado"));

        // Si el usuario es presidente, debe ceder la presidencia primero
        if (user.getRole() == Role.PRESIDENTE) {
            throw new RuntimeException("Como presidente, debes ceder la presidencia antes de cambiar de comunidad");
        }

        // Cambiar al usuario a la nueva comunidad
        user.setCommunity(nuevaComunidad);
        userRepository.save(user);

        log.info("Usuario {} cambiado exitosamente a la comunidad {}", user.getEmail(), nuevaComunidad.getName());
    }

    @Transactional
    public void cederPresidencia(User presidenteActual, CederPresidenciaRequest request) {
        log.info("Presidente {} cediendo presidencia al usuario {}", 
                presidenteActual.getEmail(), request.getIdNuevoPresidente());
        
        // Validar que el usuario actual es presidente
        if (presidenteActual.getRole() != Role.PRESIDENTE) {
            throw new RuntimeException("Solo el presidente puede ceder la presidencia");
        }

        // Buscar al nuevo presidente - refrescar desde BD
        User nuevoPresidente = userRepository.findById(request.getIdNuevoPresidente())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        log.debug("Nuevo presidente encontrado: {} - Rol actual: {}", 
                nuevoPresidente.getFullName(), nuevoPresidente.getRole());

        // Validar que el nuevo presidente pertenece a la misma comunidad
        if (!nuevoPresidente.getCommunity().getId().equals(presidenteActual.getCommunity().getId())) {
            throw new RuntimeException("El nuevo presidente debe pertenecer a la misma comunidad");
        }

        // Validar que no se está cediendo a sí mismo
        if (nuevoPresidente.getId().equals(presidenteActual.getId())) {
            throw new RuntimeException("No puedes ceder la presidencia a ti mismo");
        }

        // Validar que el nuevo presidente actualmente es vecino
        if (nuevoPresidente.getRole() != Role.VECINO) {
            throw new RuntimeException("Solo se puede ceder la presidencia a un vecino");
        }

        Community community = presidenteActual.getCommunity();

        log.debug("Antes del cambio - Presidente actual: {} ({}), Nuevo presidente: {} ({})", 
                presidenteActual.getFullName(), presidenteActual.getRole(),
                nuevoPresidente.getFullName(), nuevoPresidente.getRole());

        // Cambiar roles
        presidenteActual.setRole(Role.VECINO);
        nuevoPresidente.setRole(Role.PRESIDENTE);

        // Actualizar el presidente en la comunidad
        community.setPresident(nuevoPresidente);

        log.debug("Después del cambio - Ex-presidente: {} ({}), Nuevo presidente: {} ({})", 
                presidenteActual.getFullName(), presidenteActual.getRole(),
                nuevoPresidente.getFullName(), nuevoPresidente.getRole());

        // Guardar cambios de forma explícita
        User savedExPresidente = userRepository.save(presidenteActual);
        User savedNuevoPresidente = userRepository.save(nuevoPresidente);
        Community savedCommunity = communityRepository.save(community);

        log.debug("Guardado - Ex-presidente: {} ({}), Nuevo presidente: {} ({})", 
                savedExPresidente.getFullName(), savedExPresidente.getRole(),
                savedNuevoPresidente.getFullName(), savedNuevoPresidente.getRole());

        // Forzar flush para asegurar que los cambios se escriben inmediatamente
        userRepository.flush();
        communityRepository.flush();

        log.info("Presidencia cedida exitosamente de {} a {}", 
                presidenteActual.getEmail(), nuevoPresidente.getEmail());
    }

    /**
     * Método temporal de debugging para obtener todos los usuarios de una comunidad
     */
    @Transactional(readOnly = true)
    public List<User> obtenerUsuariosParaDebug(Community community) {
        log.debug("Debug: Obteniendo usuarios para comunidad ID: {}", community.getId());
        
        // Refrescar community desde BD
        Community freshCommunity = communityRepository.findById(community.getId())
                .orElseThrow(() -> new RuntimeException("Comunidad no encontrada"));
        
        List<User> usuarios = userRepository.findByCommunityOrderByFullName(freshCommunity);
        
        log.debug("Debug: Encontrados {} usuarios:", usuarios.size());
        for (User usuario : usuarios) {
            log.debug("  - {} ({}) - Rol: {} - Email: {}", 
                    usuario.getFullName(), usuario.getId(), usuario.getRole(), usuario.getEmail());
        }
        
        return usuarios;
    }
} 