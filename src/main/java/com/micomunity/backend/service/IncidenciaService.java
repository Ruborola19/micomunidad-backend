package com.micomunity.backend.service;

import com.micomunity.backend.dto.IncidenciaDTO;
import com.micomunity.backend.dto.IncidenciaResponse;
import com.micomunity.backend.dto.IncidenciasPaginadas;
import com.micomunity.backend.exception.BadRequestException;
import com.micomunity.backend.model.EstadoIncidencia;
import com.micomunity.backend.model.Incidencia;
import com.micomunity.backend.model.Role;
import com.micomunity.backend.model.User;
import com.micomunity.backend.repository.IncidenciaRepository;
import com.micomunity.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.micomunity.backend.exception.StorageException;

@Service
@RequiredArgsConstructor
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private static final Logger log = LoggerFactory.getLogger(IncidenciaService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public Incidencia crearIncidencia(User user, IncidenciaDTO dto, MultipartFile image) {
        log.debug("Iniciando creación de incidencia para usuario: {}", user.getEmail());
        
        // Recargar el usuario con su comunidad
        User userWithCommunity = userRepository.findById(user.getId())
            .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        if (userWithCommunity.getCommunity() == null) {
            log.error("Usuario {} no tiene comunidad asignada", userWithCommunity.getEmail());
            throw new BadRequestException("El usuario no pertenece a ninguna comunidad");
        }

        log.debug("Comunidad del usuario: {}", userWithCommunity.getCommunity().getId());

        // Inicializar la comunidad
        entityManager.refresh(userWithCommunity.getCommunity());

        Incidencia incidencia = new Incidencia();
        incidencia.setTitulo(dto.getTitulo());
        incidencia.setDescripcion(dto.getDescripcion());
        incidencia.setUbicacion(dto.getUbicacion());
        incidencia.setEstado(EstadoIncidencia.ABIERTA);
        incidencia.setCreador(userWithCommunity);
        incidencia.setCommunity(userWithCommunity.getCommunity());
        incidencia.setFechaCreacion(LocalDateTime.now());

        if (image != null && !image.isEmpty()) {
            try {
                String imagePath = fileStorageService.storeFile(image, "incidencias");
                incidencia.setImagenUrl(imagePath);
                log.debug("Imagen guardada en: {}", imagePath);
            } catch (StorageException e) {
                log.error("Error al guardar la imagen: {}", e.getMessage(), e);
                throw new RuntimeException("Error al guardar la imagen: " + e.getMessage());
            }
        }

        Incidencia savedIncidencia = incidenciaRepository.save(incidencia);
        log.debug("Incidencia creada con ID: {}", savedIncidencia.getId());
        
        return savedIncidencia;
    }

    @Transactional(readOnly = true)
    public IncidenciasPaginadas obtenerIncidenciasComunidadPaginadas(
            User user,
            int page,
            int size,
            String baseUrl,
            Sort sort
    ) {
        if (user == null || user.getCommunity() == null) {
            log.error("Usuario o comunidad no válidos. Usuario: {}, Comunidad: {}", 
                user != null ? user.getEmail() : "null",
                user != null && user.getCommunity() != null ? user.getCommunity().getId() : "null");
            throw new BadRequestException("Usuario o comunidad no válidos");
        }

        try {
            log.debug("Buscando incidencias para comunidad: {}", user.getCommunity().getId());
            Page<Incidencia> incidenciasPage = incidenciaRepository.findByCommunity(
                user.getCommunity(),
                PageRequest.of(page, size, sort)
            );
            
            log.debug("Encontradas {} incidencias en total", incidenciasPage.getTotalElements());
            log.debug("Contenido de la página actual: {} incidencias", incidenciasPage.getContent().size());

            List<IncidenciaResponse> incidencias = incidenciasPage.getContent().stream()
                .map(incidencia -> {
                    IncidenciaResponse dto = convertirADTO(incidencia, baseUrl);
                    log.debug("Convertida incidencia ID: {}, Título: {}", dto.getId(), dto.getTitulo());
                    return dto;
                })
                .toList();

            IncidenciasPaginadas response = new IncidenciasPaginadas(
                incidencias,
                incidenciasPage.getTotalPages(),
                incidenciasPage.getTotalElements(),
                incidenciasPage.getNumber(),
                incidenciasPage.getSize(),
                incidenciasPage.isFirst(),
                incidenciasPage.isLast()
            );

            log.debug("Respuesta paginada: {} incidencias, {} páginas totales", 
                response.getContent().size(), response.getTotalPages());

            return response;
        } catch (Exception e) {
            log.error("Error al obtener las incidencias: {}", e.getMessage(), e);
            throw new BadRequestException("Error al obtener las incidencias: " + e.getMessage());
        }
    }

    public Incidencia actualizarEstadoIncidencia(Long id, EstadoIncidencia nuevoEstado, User user) {
        if (user.getRole() != Role.PRESIDENTE && user.getRole() != Role.ADMINISTRADOR) {
            throw new IllegalStateException("Solo el presidente o administrador pueden actualizar el estado de las incidencias");
        }

        Incidencia incidencia = incidenciaRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("Incidencia no encontrada"));

        if (!user.getCommunity().getId().equals(incidencia.getCommunity().getId())) {
            throw new IllegalStateException("No tienes permiso para actualizar esta incidencia");
        }

        if (incidencia.getEstado() == EstadoIncidencia.CANCELADA) {
            throw new IllegalStateException("No se puede modificar una incidencia cancelada");
        }

        // Validar secuencia de estados
        switch (incidencia.getEstado()) {
            case ABIERTA:
                if (nuevoEstado != EstadoIncidencia.EN_PROCESO && nuevoEstado != EstadoIncidencia.CANCELADA) {
                    throw new IllegalStateException("Desde ABIERTA solo se puede pasar a EN_PROCESO o CANCELADA");
                }
                break;
            case EN_PROCESO:
                if (nuevoEstado != EstadoIncidencia.RESUELTA && nuevoEstado != EstadoIncidencia.CANCELADA) {
                    throw new IllegalStateException("Desde EN_PROCESO solo se puede pasar a RESUELTA o CANCELADA");
                }
                break;
            case RESUELTA:
                if (nuevoEstado != EstadoIncidencia.CANCELADA) {
                    throw new IllegalStateException("Desde RESUELTA solo se puede pasar a CANCELADA");
                }
                break;
            default:
                throw new IllegalStateException("Estado actual no válido");
        }

        incidencia.setEstado(nuevoEstado);
        return incidenciaRepository.saveAndFlush(incidencia);
    }

    public void eliminarIncidencia(Long id, User user) {
        Incidencia incidencia = incidenciaRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("Incidencia no encontrada"));

        boolean isAdmin = user.getRole() == Role.ADMINISTRADOR;
        boolean isPresident = user.getRole() == Role.PRESIDENTE;
        boolean isAuthor = incidencia.getCreador().getId().equals(user.getId());
        boolean isSameCommunity = user.getCommunity().getId().equals(incidencia.getCommunity().getId());

        if (!isSameCommunity) {
            throw new IllegalStateException("No tienes permiso para eliminar esta incidencia");
        }

        if (!isAdmin && !isPresident && !isAuthor) {
            throw new IllegalStateException("Solo el administrador, presidente o el autor pueden eliminar esta incidencia");
        }

        if (incidencia.getImagenUrl() != null) {
            try {
                fileStorageService.deleteFile(incidencia.getImagenUrl());
                log.debug("Imagen eliminada: {}", incidencia.getImagenUrl());
            } catch (Exception e) {
                log.warn("Error al eliminar la imagen: {}", e.getMessage());
            }
        }

        incidenciaRepository.delete(incidencia);
        log.info("Incidencia {} eliminada por usuario {}", id, user.getEmail());
    }

    public IncidenciaResponse convertirADTO(Incidencia incidencia, String baseUrl) {
        IncidenciaResponse dto = new IncidenciaResponse();
        dto.setId(incidencia.getId());
        dto.setTitulo(incidencia.getTitulo());
        dto.setDescripcion(incidencia.getDescripcion());
        dto.setUbicacion(incidencia.getUbicacion());
        dto.setFechaCreacion(incidencia.getFechaCreacion().toString());
        dto.setAutorId(incidencia.getCreador().getId());
        dto.setAutorNombre(incidencia.getCreador().getFullName());
        dto.setCodigoComunidad(incidencia.getCommunity().getCommunityCode());
        dto.setEstado(incidencia.getEstado());
        
        if (incidencia.getImagenUrl() != null && !incidencia.getImagenUrl().isEmpty()) {
            // Asegurarnos de usar el puerto 8080 para el backend
            String backendUrl = baseUrl.replace(":3000", ":8080");
            String imageUrl = backendUrl + "/api/incidencias/download/" + incidencia.getImagenUrl();
            log.debug("URL de imagen construida: {}", imageUrl);
            dto.setImagenUrl(imageUrl);
        }
        
        return dto;
    }
} 