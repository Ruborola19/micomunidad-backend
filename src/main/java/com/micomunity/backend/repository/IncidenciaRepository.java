package com.micomunity.backend.repository;

import com.micomunity.backend.model.Community;
import com.micomunity.backend.model.Incidencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
    @Query("SELECT i FROM Incidencia i " +
           "LEFT JOIN FETCH i.creador " +
           "LEFT JOIN FETCH i.community " +
           "WHERE i.community.id = :communityId")
    List<Incidencia> findByCommunityId(@Param("communityId") Long communityId);

    @Query("SELECT i FROM Incidencia i " +
           "LEFT JOIN FETCH i.creador " +
           "LEFT JOIN FETCH i.community " +
           "WHERE i.community.id = :communityId " +
           "ORDER BY i.fechaCreacion DESC")
    List<Incidencia> findByCommunityIdOrderByFechaCreacionDesc(@Param("communityId") Long communityId);

    @Query(value = "SELECT i FROM Incidencia i " +
           "LEFT JOIN FETCH i.creador " +
           "LEFT JOIN FETCH i.community " +
           "WHERE i.community = :community",
           countQuery = "SELECT COUNT(i) FROM Incidencia i WHERE i.community = :community")
    Page<Incidencia> findByCommunity(@Param("community") Community community, Pageable pageable);
} 