package com.micomunity.backend.service;

import com.micomunity.backend.dto.ReservaRequest;
import com.micomunity.backend.dto.ReservaResponse;
import com.micomunity.backend.dto.CalendarioReservasResponse;
import com.micomunity.backend.dto.MisReservasResponse;
import com.micomunity.backend.dto.HorariosDisponiblesResponse;
import com.micomunity.backend.model.*;
import com.micomunity.backend.repository.ReservaRepository;
import com.micomunity.backend.repository.ZonaComunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final ZonaComunRepository zonaComunRepository;

    @Value("${reservas.limite.por.usuario:2}")
    private int limiteReservasPorUsuario;

    @Value("${reservas.limite.por.zona:1}")
    private int limiteReservasPorZona;

    @Transactional
    public ReservaResponse crearReserva(User user, ReservaRequest request) {
        log.info("Usuario {} creando reserva para zona {}", user.getEmail(), request.getZonaComunId());
        
        // Validar que el usuario es vecino o presidente
        if (user.getRole() != Role.VECINO && user.getRole() != Role.PRESIDENTE) {
            throw new RuntimeException("Solo los vecinos y el presidente pueden realizar reservas");
        }

        // Validar que la zona común existe y pertenece a la comunidad del usuario
        ZonaComun zonaComun = zonaComunRepository.findByIdAndCommunity(
                request.getZonaComunId(), user.getCommunity())
                .orElseThrow(() -> new RuntimeException("Zona común no encontrada o no pertenece a tu comunidad"));

        // Validaciones de fecha y hora
        validarFechaYHora(request);

        // Validar que no hay conflictos horarios
        validarConflictosHorarios(zonaComun, request);

        // Validar límites de reservas
        validarLimitesReservas(user, zonaComun, request.getFecha());

        // Crear la reserva
        Reserva reserva = new Reserva(zonaComun, user, request.getFecha(), 
                request.getHoraInicio(), request.getHoraFin());
        reserva = reservaRepository.save(reserva);

        log.info("Reserva creada exitosamente: ID={} para zona {} el {}", 
                reserva.getId(), zonaComun.getNombre(), request.getFecha());
        
        return convertToResponse(reserva, user);
    }

    @Transactional
    public void cancelarReserva(User user, UUID reservaId) {
        log.info("Usuario {} cancelando reserva {}", user.getEmail(), reservaId);
        
        // Buscar la reserva y validar que pertenece al usuario
        Reserva reserva = reservaRepository.findByIdAndUsuario(reservaId, user)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada o no te pertenece"));

        // Validar que la reserva no haya pasado
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fechaHoraReserva = LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio());
        
        if (fechaHoraReserva.isBefore(ahora)) {
            throw new RuntimeException("No se puede cancelar una reserva que ya ha pasado");
        }

        // Validar que la reserva está activa
        if (reserva.getEstado() != EstadoReserva.ACTIVA) {
            throw new RuntimeException("La reserva ya está cancelada");
        }

        // Cancelar la reserva
        reserva.cancelar();
        reservaRepository.save(reserva);

        log.info("Reserva cancelada exitosamente: ID={}", reservaId);
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> obtenerReservasDeZona(User user, UUID zonaId) {
        log.info("Usuario {} obteniendo reservas de zona {}", user.getEmail(), zonaId);
        
        // Validar que la zona común existe y pertenece a la comunidad del usuario
        ZonaComun zonaComun = zonaComunRepository.findByIdAndCommunity(zonaId, user.getCommunity())
                .orElseThrow(() -> new RuntimeException("Zona común no encontrada o no pertenece a tu comunidad"));

        List<Reserva> reservas = reservaRepository.findByZonaComunAndEstadoActivaOrderByFechaAndHora(zonaComun);
        
        log.info("Encontradas {} reservas activas para la zona {}", reservas.size(), zonaComun.getNombre());
        
        return reservas.stream()
                .map(reserva -> convertToResponse(reserva, user))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> obtenerHistorialReservas(User user, UUID zonaId, 
                                                         LocalDate fechaInicio, LocalDate fechaFin) {
        log.info("Usuario {} obteniendo historial de reservas", user.getEmail());
        
        // Validar que el usuario es presidente
        if (user.getRole() != Role.PRESIDENTE) {
            throw new RuntimeException("Solo el presidente puede consultar el historial de reservas");
        }

        Community community = user.getCommunity();
        List<Reserva> reservas = reservaRepository.findHistorialReservas(community, zonaId, fechaInicio, fechaFin);
        
        log.info("Encontradas {} reservas en el historial", reservas.size());
        
        return reservas.stream()
                .map(reserva -> convertToResponse(reserva, user))
                .collect(Collectors.toList());
    }

    private void validarFechaYHora(ReservaRequest request) {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();

        // No se permiten reservas en fechas pasadas
        if (request.getFecha().isBefore(hoy)) {
            throw new RuntimeException("No se pueden realizar reservas en fechas pasadas");
        }

        // Si es hoy, no se permiten reservas en horas pasadas
        if (request.getFecha().equals(hoy) && request.getHoraInicio().isBefore(ahora)) {
            throw new RuntimeException("No se pueden realizar reservas en horas pasadas");
        }

        // La hora de fin debe ser posterior a la hora de inicio
        if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new RuntimeException("La hora de fin debe ser posterior a la hora de inicio");
        }
    }

    private void validarConflictosHorarios(ZonaComun zonaComun, ReservaRequest request) {
        List<Reserva> conflictos = reservaRepository.findConflictingReservations(
                zonaComun, request.getFecha(), request.getHoraInicio(), request.getHoraFin());
        
        if (!conflictos.isEmpty()) {
            throw new RuntimeException("Ya existe una reserva en ese horario para la zona común");
        }
    }

    private void validarLimitesReservas(User user, ZonaComun zonaComun, LocalDate fecha) {
        // Verificar límite por zona para la fecha específica
        int reservasEnZona = reservaRepository.countActiveReservasByUsuarioAndZonaAndFecha(user, zonaComun, fecha);
        if (reservasEnZona >= limiteReservasPorZona) {
            throw new RuntimeException("Has alcanzado el límite de reservas para esta zona común en esta fecha (" + 
                    limiteReservasPorZona + ")");
        }

        // Verificar límite total del usuario (comentado para permitir reservas en diferentes fechas)
        // int reservasTotal = reservaRepository.countActiveReservasByUsuario(user);
        // if (reservasTotal >= limiteReservasPorUsuario) {
        //     throw new RuntimeException("Has alcanzado el límite de reservas activas totales (" + 
        //             limiteReservasPorUsuario + ")");
        // }
    }

    @Transactional(readOnly = true)
    public List<MisReservasResponse> obtenerMisReservas(User user) {
        log.info("Usuario {} obteniendo sus reservas", user.getEmail());
        
        List<Reserva> reservas = reservaRepository.findByUsuarioOrderByFechaDesc(user);
        
        log.info("Encontradas {} reservas para el usuario", reservas.size());
        
        return reservas.stream()
                .map(this::convertToMisReservasResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CalendarioReservasResponse obtenerCalendarioZona(User user, UUID zonaId, LocalDate fecha) {
        log.info("Usuario {} obteniendo calendario de zona {} para fecha {}", 
                user.getEmail(), zonaId, fecha);
        
        // Validar que la zona común existe y pertenece a la comunidad del usuario
        ZonaComun zonaComun = zonaComunRepository.findByIdAndCommunity(zonaId, user.getCommunity())
                .orElseThrow(() -> new RuntimeException("Zona común no encontrada o no pertenece a tu comunidad"));

        List<Reserva> reservas = reservaRepository.findByZonaComunAndFechaAndEstadoActiva(zonaComun, fecha);
        
        // Generar horarios disponibles (de 8:00 a 22:00, en bloques de 2 horas)
        List<String> horasDisponibles = generarHorasDisponibles(reservas);
        
        List<CalendarioReservasResponse.ReservaCalendarioDTO> reservasCalendario = reservas.stream()
                .map(reserva -> convertToReservaCalendario(reserva, user))
                .collect(Collectors.toList());

        return new CalendarioReservasResponse(
                zonaComun.getId(),
                zonaComun.getNombre(),
                fecha,
                reservasCalendario,
                horasDisponibles
        );
    }
    
    @Transactional(readOnly = true)
    public List<ReservaResponse> obtenerReservasComunidad(User user, LocalDate fechaInicio, LocalDate fechaFin) {
        log.info("Usuario {} obteniendo reservas de la comunidad del {} al {}", 
                user.getEmail(), fechaInicio, fechaFin);
        
        Community community = user.getCommunity();
        
        // Si no se especifican fechas, usar la semana actual
        if (fechaInicio == null) {
            fechaInicio = LocalDate.now();
        }
        if (fechaFin == null) {
            fechaFin = fechaInicio.plusDays(7);
        }
        
        List<Reserva> reservas = reservaRepository.findReservasComunidadByFechaRange(
                community, fechaInicio, fechaFin);
        
        log.info("Encontradas {} reservas en la comunidad", reservas.size());
        
        return reservas.stream()
                .map(reserva -> convertToResponse(reserva, user))
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public HorariosDisponiblesResponse obtenerHorariosDisponibles(User user, UUID zonaId, LocalDate fecha) {
        log.info("Usuario {} obteniendo horarios disponibles para zona {} el {}", 
                user.getEmail(), zonaId, fecha);
        
        // Validar que la zona común existe y pertenece a la comunidad del usuario
        ZonaComun zonaComun = zonaComunRepository.findByIdAndCommunity(zonaId, user.getCommunity())
                .orElseThrow(() -> new RuntimeException("Zona común no encontrada o no pertenece a tu comunidad"));

        List<Reserva> reservasExistentes = reservaRepository.findByZonaComunAndFechaAndEstadoActiva(zonaComun, fecha);
        
        List<HorariosDisponiblesResponse.HorarioDisponible> horarios = generarHorariosCompletos(reservasExistentes, fecha);
        
        return new HorariosDisponiblesResponse(
                zonaComun.getId(),
                zonaComun.getNombre(),
                fecha,
                horarios
        );
    }

    private List<HorariosDisponiblesResponse.HorarioDisponible> generarHorariosCompletos(
            List<Reserva> reservasExistentes, LocalDate fecha) {
        
        List<String[]> horariosCompletos = List.of(
                new String[]{"08:00", "10:00"},
                new String[]{"10:00", "12:00"},
                new String[]{"12:00", "14:00"},
                new String[]{"14:00", "16:00"},
                new String[]{"16:00", "18:00"},
                new String[]{"18:00", "20:00"},
                new String[]{"20:00", "22:00"}
        );
        
        return horariosCompletos.stream()
                .map(horario -> {
                    String horaInicio = horario[0];
                    String horaFin = horario[1];
                    
                    boolean disponible = reservasExistentes.stream()
                            .noneMatch(r -> r.getHoraInicio().toString().equals(horaInicio));
                    
                    String motivo = null;
                    if (!disponible) {
                        motivo = "Ya reservado";
                    } else if (fecha.equals(LocalDate.now()) && 
                               LocalTime.parse(horaInicio).isBefore(LocalTime.now())) {
                        disponible = false;
                        motivo = "Hora pasada";
                    } else if (fecha.isBefore(LocalDate.now())) {
                        disponible = false;
                        motivo = "Fecha pasada";
                    }
                    
                    return new HorariosDisponiblesResponse.HorarioDisponible(
                            horaInicio, horaFin, disponible, motivo
                    );
                })
                .collect(Collectors.toList());
    }

    private List<String> generarHorasDisponibles(List<Reserva> reservasExistentes) {
        List<String> todasLasHoras = List.of(
                "08:00", "10:00", "12:00", "14:00", "16:00", "18:00", "20:00"
        );
        
        List<String> horasOcupadas = reservasExistentes.stream()
                .map(r -> r.getHoraInicio().toString())
                .collect(Collectors.toList());
        
        return todasLasHoras.stream()
                .filter(hora -> !horasOcupadas.contains(hora))
                .collect(Collectors.toList());
    }

    private CalendarioReservasResponse.ReservaCalendarioDTO convertToReservaCalendario(Reserva reserva, User currentUser) {
        CalendarioReservasResponse.ReservaCalendarioDTO dto = new CalendarioReservasResponse.ReservaCalendarioDTO();
        dto.setId(reserva.getId());
        dto.setHoraInicio(reserva.getHoraInicio().toString());
        dto.setHoraFin(reserva.getHoraFin().toString());
        
        // Mostrar nombre solo si es el presidente o la reserva es propia
        if (currentUser.getRole() == Role.PRESIDENTE || 
            currentUser.getId().equals(reserva.getUsuario().getId())) {
            dto.setUsuarioNombre(reserva.getUsuario().getFullName());
        } else {
            dto.setUsuarioNombre("Reservado");
        }
        
        dto.setEsPropia(currentUser.getId().equals(reserva.getUsuario().getId()));
        dto.setPuedeCancelar(
            dto.isEsPropia() && 
            reserva.getEstado() == EstadoReserva.ACTIVA &&
            !LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio()).isBefore(LocalDateTime.now())
        );
        
        return dto;
    }

    private MisReservasResponse convertToMisReservasResponse(Reserva reserva) {
        MisReservasResponse response = new MisReservasResponse();
        response.setId(reserva.getId());
        response.setZonaComunId(reserva.getZonaComun().getId());
        response.setZonaComunNombre(reserva.getZonaComun().getNombre());
        response.setFecha(reserva.getFecha());
        response.setHoraInicio(reserva.getHoraInicio());
        response.setHoraFin(reserva.getHoraFin());
        response.setEstado(reserva.getEstado());
        response.setFechaCreacion(reserva.getFechaCreacion());
        response.setFechaCancelacion(reserva.getFechaCancelacion());

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicioReserva = LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio());
        LocalDateTime finReserva = LocalDateTime.of(reserva.getFecha(), reserva.getHoraFin());

        response.setYaComenzo(inicioReserva.isBefore(ahora));
        response.setYaTermino(finReserva.isBefore(ahora));
        response.setPuedeCancelar(
            reserva.getEstado() == EstadoReserva.ACTIVA && 
            inicioReserva.isAfter(ahora)
        );
        
        // Calcular horas hasta el inicio
        if (inicioReserva.isAfter(ahora)) {
            response.setHorasHastaInicio(java.time.Duration.between(ahora, inicioReserva).toHours());
        } else {
            response.setHorasHastaInicio(0);
        }

        return response;
    }

    private ReservaResponse convertToResponse(Reserva reserva, User currentUser) {
        ReservaResponse response = new ReservaResponse();
        response.setId(reserva.getId());
        response.setZonaComunId(reserva.getZonaComun().getId());
        response.setZonaComunNombre(reserva.getZonaComun().getNombre());
        response.setUsuarioId(reserva.getUsuario().getId());
        response.setFecha(reserva.getFecha());
        response.setHoraInicio(reserva.getHoraInicio());
        response.setHoraFin(reserva.getHoraFin());
        response.setEstado(reserva.getEstado());
        response.setFechaCreacion(reserva.getFechaCreacion());
        response.setFechaCancelacion(reserva.getFechaCancelacion());

        // Datos del usuario que hizo la reserva (según el rol del usuario que consulta)
        if (currentUser.getRole() == Role.PRESIDENTE || 
            currentUser.getId().equals(reserva.getUsuario().getId())) {
            // El presidente y el dueño de la reserva pueden ver todos los datos
            response.setUsuarioNombre(reserva.getUsuario().getFullName());
            response.setUsuarioEmail(reserva.getUsuario().getEmail());
        } else {
            // Otros usuarios solo ven datos básicos
            response.setUsuarioNombre("Reservado");
            response.setUsuarioEmail("");
        }

        // Solo el usuario que hizo la reserva puede cancelarla (vecino o presidente)
        response.setPuedeCancelar(
            (currentUser.getRole() == Role.VECINO || currentUser.getRole() == Role.PRESIDENTE) && 
            currentUser.getId().equals(reserva.getUsuario().getId()) &&
            reserva.getEstado() == EstadoReserva.ACTIVA &&
            !LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio()).isBefore(LocalDateTime.now())
        );

        return response;
    }
} 