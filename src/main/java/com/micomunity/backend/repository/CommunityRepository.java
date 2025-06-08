package com.micomunity.backend.repository;

import com.micomunity.backend.model.Community;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Long> {
    Optional<Community> findByCommunityCode(String communityCode);
    boolean existsByCommunityCode(String communityCode);
} 