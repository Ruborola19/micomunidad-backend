package com.micomunity.backend.service;

import com.micomunity.backend.dto.ZonaComunRequest;
import com.micomunity.backend.dto.ZonaComunResponse;
import com.micomunity.backend.model.Community;
import com.micomunity.backend.model.Role;
import com.micomunity.backend.model.User;
import com.micomunity.backend.model.ZonaComun;
import com.micomunity.backend.repository.ReservaRepository;
import com.micomunity.backend.repository.ZonaComunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ZonaComunService {

    private final ZonaComunRepository zonaComunRepository;
    private final ReservaRepository reservaRepository;

    @Transactional
    public ZonaComunResponse crearZonaComun(User user, ZonaComunRequest request) {
        log.info("Usuario {} creando zona común: {}", user.getEmail(), request.getNombre());
        
        // Validar que el usuario es presidente
        if (user.getRole() != Role.PRESIDENTE) {
            throw new RuntimeException("Solo el presidente puede crear zonas comunes");
        }

        Community community = user.getCommunity();
        if (community == null) {
            throw new RuntimeException("Usuario sin comunidad asignada");
        }

        // Validar que no exista una zona con el mismo nombre en la comunidad
        if (zonaComunRepository.existsByNombreAndCommunity(request.getNombre(), community)) {
            throw new RuntimeException("Ya existe una zona común con ese nombre en la comunidad");
        }

        // Crear la zona común
        ZonaComun zonaComun = new ZonaComun(request.getNombre(), community);
        zonaComun = zonaComunRepository.save(zonaComun);

        log.info("Zona común creada exitosamente: ID={}, Nombre={}", zonaComun.getId(), zonaComun.getNombre());
        return convertToResponse(zonaComun, user);
    }

    @Transactional
    public void eliminarZonaComun(User user, UUID zonaId) {
        log.info("Usuario {} eliminando zona común: {}", user.getEmail(), zonaId);
        
        // Validar que el usuario es presidente
        if (user.getRole() != Role.PRESIDENTE) {
            throw new RuntimeException("Solo el presidente puede eliminar zonas comunes");
        }

        Community community = user.getCommunity();
        ZonaComun zonaComun = zonaComunRepository.findByIdAndCommunity(zonaId, community)
                .orElseThrow(() -> new RuntimeException("Zona común no encontrada o no pertenece a tu comunidad"));

        // Verificar que no hay reservas futuras activas
        int reservasFuturas = reservaRepository.countFutureActiveReservasByZona(zonaComun);
        if (reservasFuturas > 0) {
            throw new RuntimeException("No se puede eliminar la zona común porque tiene reservas futuras activas");
        }

        zonaComunRepository.delete(zonaComun);
        log.info("Zona común eliminada exitosamente: ID={}, Nombre={}", zonaComun.getId(), zonaComun.getNombre());
    }

    @Transactional(readOnly = true)
    public List<ZonaComunResponse> obtenerZonasComunes(User user) {
        log.info("Usuario {} obteniendo zonas comunes", user.getEmail());
        
        Community community = user.getCommunity();
        if (community == null) {
            throw new RuntimeException("Usuario sin comunidad asignada");
        }

        List<ZonaComun> zonasComunes = zonaComunRepository.findByCommunityOrderByNombre(community);
        
        log.info("Encontradas {} zonas comunes para la comunidad {}", 
                zonasComunes.size(), community.getCommunityCode());
        
        return zonasComunes.stream()
                .map(zona -> convertToResponse(zona, user))
                .collect(Collectors.toList());
    }

    private ZonaComunResponse convertToResponse(ZonaComun zonaComun, User currentUser) {
        ZonaComunResponse response = new ZonaComunResponse();
        response.setId(zonaComun.getId());
        response.setNombre(zonaComun.getNombre());
        response.setCommunityCode(zonaComun.getCommunity().getCommunityCode());
        response.setCommunityId(zonaComun.getCommunity().getId());
        
        // Solo el presidente puede eliminar zonas comunes
        response.setPuedeEliminar(currentUser.getRole() == Role.PRESIDENTE);
        
        return response;
    }
} 