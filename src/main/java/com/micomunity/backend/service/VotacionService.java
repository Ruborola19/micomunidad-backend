package com.micomunity.backend.service;

import com.micomunity.backend.dto.CrearVotacionDTO;
import com.micomunity.backend.dto.VotacionResponseDTO;
import com.micomunity.backend.dto.VotoRequestDTO;
import com.micomunity.backend.model.*;
import com.micomunity.backend.repository.CommunityRepository;
import com.micomunity.backend.repository.UserRepository;
import com.micomunity.backend.repository.VotacionRepository;
import com.micomunity.backend.repository.VotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VotacionService {

    private final VotacionRepository votacionRepository;
    private final VotoRepository votoRepository;
    private final UserRepository userRepository;
    private final CommunityRepository communityRepository;

    public void crearVotacion(CrearVotacionDTO dto, Principal principal) {
        User creador = getUsuarioActual(principal);

        if (creador.getRole() != Role.PRESIDENTE) {
            throw new RuntimeException("Solo el PRESIDENTE puede crear votaciones");
        }

        Votacion votacion = new Votacion();
        votacion.setTitulo(dto.getTitulo());
        votacion.setDescripcion(dto.getDescripcion());
        votacion.setOpcion1(dto.getOpcion1());
        votacion.setOpcion2(dto.getOpcion2());
        votacion.setOpcion3(dto.getOpcion3());
        votacion.setDuracionHoras(dto.getDuracionHoras());
        votacion.setFechaCreacion(LocalDateTime.now());
        votacion.setFechaFinal(LocalDateTime.now().plusHours(dto.getDuracionHoras()));
        votacion.setCreador(creador);
        votacion.setComunidad(creador.getCommunity());

        votacionRepository.save(votacion);
    }

    @Transactional
    public void votar(VotoRequestDTO dto, Principal principal) {
        User usuario = getUsuarioActual(principal);
        Votacion votacion = votacionRepository.findById(dto.getVotacionId())
                .orElseThrow(() -> new RuntimeException("Votación no encontrada"));

        if (!votacion.getComunidad().equals(usuario.getCommunity())) {
            throw new RuntimeException("No puedes votar en una votación de otra comunidad");
        }

        if (LocalDateTime.now().isAfter(votacion.getFechaFinal())) {
            throw new RuntimeException("La votación ya ha finalizado");
        }

        Optional<Voto> existente = votoRepository.findByVotanteAndVotacion(usuario, votacion);
        if (existente.isPresent()) {
            throw new RuntimeException("Ya has votado en esta votación");
        }

        String opcion = dto.getOpcionSeleccionada();
        if (!List.of(votacion.getOpcion1(), votacion.getOpcion2(), votacion.getOpcion3()).contains(opcion)) {
            throw new RuntimeException("Opción no válida");
        }

        Voto voto = new Voto();
        voto.setVotante(usuario);
        voto.setVotacion(votacion);
        voto.setOpcionSeleccionada(opcion);
        voto.setClaveUnica(usuario.getId() + "_" + votacion.getId());

        votoRepository.save(voto);
    }

    public List<VotacionResponseDTO> obtenerVotacionesActivas(Principal principal) {
        User usuario = getUsuarioActual(principal);
        List<Votacion> activas = votacionRepository.findByComunidadAndFechaFinalAfter(
                usuario.getCommunity(), LocalDateTime.now());

        return activas.stream()
                .map(v -> toResponseDTO(v, usuario))
                .collect(Collectors.toList());
    }

    public List<VotacionResponseDTO> obtenerVotacionesCerradas(Principal principal) {
        User usuario = getUsuarioActual(principal);
        List<Votacion> cerradas = votacionRepository.findByComunidadAndFechaFinalBefore(
                usuario.getCommunity(), LocalDateTime.now());

        return cerradas.stream()
                .map(v -> toResponseDTO(v, usuario))
                .collect(Collectors.toList());
    }

    public void eliminarVotacion(Long id, Principal principal) {
        User usuario = getUsuarioActual(principal);
        Votacion votacion = votacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Votación no encontrada"));

        if (!votacion.getCreador().getId().equals(usuario.getId()) || usuario.getRole() != Role.PRESIDENTE) {
            throw new RuntimeException("Solo el PRESIDENTE que creó la votación puede eliminarla");
        }

        votacionRepository.delete(votacion);
    }

    private VotacionResponseDTO toResponseDTO(Votacion votacion, User usuario) {
        VotacionResponseDTO dto = new VotacionResponseDTO();
        dto.setId(votacion.getId());
        dto.setTitulo(votacion.getTitulo());
        dto.setDescripcion(votacion.getDescripcion());
        dto.setOpcion1(votacion.getOpcion1());
        dto.setOpcion2(votacion.getOpcion2());
        dto.setOpcion3(votacion.getOpcion3());
        dto.setFechaCreacion(votacion.getFechaCreacion());
        dto.setFechaFinal(votacion.getFechaFinal());
        dto.setFinalizada(LocalDateTime.now().isAfter(votacion.getFechaFinal()));

        boolean yaVotado = votoRepository.findByVotanteAndVotacion(usuario, votacion).isPresent();
        dto.setYaVotado(yaVotado);

        if (yaVotado || dto.isFinalizada()) {
            List<Voto> votos = votoRepository.findByVotacion(votacion);
            Map<String, Long> resultados = votos.stream()
                    .collect(Collectors.groupingBy(Voto::getOpcionSeleccionada, Collectors.counting()));
            dto.setResultados(resultados);
        }

        return dto;
    }

    private User getUsuarioActual(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}
