package com.micomunity.backend.repository;

import com.micomunity.backend.model.Community;
import com.micomunity.backend.model.ZonaComun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ZonaComunRepository extends JpaRepository<ZonaComun, UUID> {
    
    // Buscar todas las zonas comunes de una comunidad
    @Query("SELECT z FROM ZonaComun z WHERE z.community = :community ORDER BY z.nombre")
    List<ZonaComun> findByCommunityOrderByNombre(@Param("community") Community community);
    
    // Verificar si existe una zona con el mismo nombre en la misma comunidad
    boolean existsByNombreAndCommunity(String nombre, Community community);
    
    // Buscar zona por ID y comunidad (para validaciones de seguridad)
    Optional<ZonaComun> findByIdAndCommunity(UUID id, Community community);
    
    // Buscar zona por ID con fetch de reservas activas futuras
    @Query("SELECT z FROM ZonaComun z " +
           "LEFT JOIN FETCH z.reservas r " +
           "WHERE z.id = :id " +
           "AND (r.estado = 'ACTIVA' AND r.fecha >= CURRENT_DATE OR r IS NULL)")
    Optional<ZonaComun> findByIdWithFutureActiveReservas(@Param("id") UUID id);
} 