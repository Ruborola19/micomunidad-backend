package com.micomunity.backend.repository;

import com.micomunity.backend.model.ChatMessage;
import com.micomunity.backend.model.Community;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * Obtener los últimos N mensajes de una comunidad específica, ordenados por timestamp
     */
    @Query("SELECT c FROM ChatMessage c " +
           "WHERE c.community = :community " +
           "ORDER BY c.timestamp DESC")
    List<ChatMessage> findLatestMessagesByCommunity(@Param("community") Community community, Pageable pageable);
    
    /**
     * Obtener todos los mensajes de una comunidad ordenados por timestamp ascendente
     */
    @Query("SELECT c FROM ChatMessage c " +
           "WHERE c.community = :community " +
           "ORDER BY c.timestamp ASC")
    List<ChatMessage> findAllMessagesByCommunityOrderByTimestamp(@Param("community") Community community);
    
    /**
     * Obtener los últimos N mensajes de una comunidad ordenados cronológicamente (para historial)
     */
    @Query("SELECT c FROM ChatMessage c " +
           "WHERE c.community = :community " +
           "ORDER BY c.timestamp DESC")
    List<ChatMessage> findLatestMessagesByCommunityForHistory(@Param("community") Community community, Pageable pageable);
} 