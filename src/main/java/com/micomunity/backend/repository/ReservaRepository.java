package com.micomunity.backend.repository;

import com.micomunity.backend.model.EstadoReserva;
import com.micomunity.backend.model.Reserva;
import com.micomunity.backend.model.User;
import com.micomunity.backend.model.ZonaComun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, UUID> {
    
    // Verificar conflictos horarios en una zona específica
    @Query("SELECT r FROM Reserva r " +
           "WHERE r.zonaComun = :zonaComun " +
           "AND r.fecha = :fecha " +
           "AND r.estado = 'ACTIVA' " +
           "AND ((r.horaInicio < :horaFin AND r.horaFin > :horaInicio))")
    List<Reserva> findConflictingReservations(
        @Param("zonaComun") ZonaComun zonaComun,
        @Param("fecha") LocalDate fecha,
        @Param("horaInicio") LocalTime horaInicio,
        @Param("horaFin") LocalTime horaFin
    );
    
    // Obtener reservas de una zona específica ordenadas por fecha y hora
    @Query("SELECT r FROM Reserva r " +
           "LEFT JOIN FETCH r.usuario " +
           "WHERE r.zonaComun = :zonaComun " +
           "AND r.estado = 'ACTIVA' " +
           "ORDER BY r.fecha, r.horaInicio")
    List<Reserva> findByZonaComunAndEstadoActivaOrderByFechaAndHora(@Param("zonaComun") ZonaComun zonaComun);
    
    // Contar reservas activas de un usuario en una zona específica para una fecha específica
    @Query("SELECT COUNT(r) FROM Reserva r " +
           "WHERE r.usuario = :usuario " +
           "AND r.zonaComun = :zonaComun " +
           "AND r.estado = 'ACTIVA' " +
           "AND r.fecha = :fecha")
    int countActiveReservasByUsuarioAndZonaAndFecha(@Param("usuario") User usuario, @Param("zonaComun") ZonaComun zonaComun, @Param("fecha") LocalDate fecha);
    
    // Contar reservas activas de un usuario en toda su comunidad
    @Query("SELECT COUNT(r) FROM Reserva r " +
           "WHERE r.usuario = :usuario " +
           "AND r.estado = 'ACTIVA' " +
           "AND r.fecha >= CURRENT_DATE")
    int countActiveReservasByUsuario(@Param("usuario") User usuario);
    
    // Buscar reserva por ID y usuario (para validaciones de propiedad)
    Optional<Reserva> findByIdAndUsuario(UUID id, User usuario);
    
    // Historial de reservas de una comunidad (para presidente)
    @Query("SELECT r FROM Reserva r " +
           "LEFT JOIN FETCH r.usuario " +
           "LEFT JOIN FETCH r.zonaComun " +
           "WHERE r.zonaComun.community = :community " +
           "AND (:zonaId IS NULL OR r.zonaComun.id = :zonaId) " +
           "AND (:fechaInicio IS NULL OR r.fecha >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR r.fecha <= :fechaFin) " +
           "ORDER BY r.fecha DESC, r.horaInicio DESC")
    List<Reserva> findHistorialReservas(
        @Param("community") com.micomunity.backend.model.Community community,
        @Param("zonaId") UUID zonaId,
        @Param("fechaInicio") LocalDate fechaInicio,
        @Param("fechaFin") LocalDate fechaFin
    );
    
    // Reservas futuras activas de una zona (para verificar antes de eliminar zona)
    @Query("SELECT COUNT(r) FROM Reserva r " +
           "WHERE r.zonaComun = :zonaComun " +
           "AND r.estado = 'ACTIVA' " +
           "AND r.fecha > CURRENT_DATE")
    int countFutureActiveReservasByZona(@Param("zonaComun") ZonaComun zonaComun);
    
    // Obtener reservas de un usuario específico
    @Query("SELECT r FROM Reserva r " +
           "LEFT JOIN FETCH r.zonaComun " +
           "WHERE r.usuario = :usuario " +
           "ORDER BY r.fecha DESC, r.horaInicio DESC")
    List<Reserva> findByUsuarioOrderByFechaDesc(@Param("usuario") User usuario);
    
    // Obtener reservas de una zona para una fecha específica
    @Query("SELECT r FROM Reserva r " +
           "LEFT JOIN FETCH r.usuario " +
           "WHERE r.zonaComun = :zonaComun " +
           "AND r.fecha = :fecha " +
           "AND r.estado = 'ACTIVA' " +
           "ORDER BY r.horaInicio")
    List<Reserva> findByZonaComunAndFechaAndEstadoActiva(
        @Param("zonaComun") ZonaComun zonaComun, 
        @Param("fecha") LocalDate fecha
    );
    
    // Obtener todas las reservas activas de la comunidad para un rango de fechas
    @Query("SELECT r FROM Reserva r " +
           "LEFT JOIN FETCH r.usuario " +
           "LEFT JOIN FETCH r.zonaComun " +
           "WHERE r.zonaComun.community = :community " +
           "AND r.estado = 'ACTIVA' " +
           "AND r.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "ORDER BY r.fecha, r.horaInicio")
    List<Reserva> findReservasComunidadByFechaRange(
        @Param("community") com.micomunity.backend.model.Community community,
        @Param("fechaInicio") LocalDate fechaInicio,
        @Param("fechaFin") LocalDate fechaFin
    );
} 