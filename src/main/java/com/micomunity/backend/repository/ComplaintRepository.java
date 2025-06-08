package com.micomunity.backend.repository;

import com.micomunity.backend.model.Community;
import com.micomunity.backend.model.Complaint;
import com.micomunity.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Page<Complaint> findByUser(User user, Pageable pageable);

    long countByUser(User user);

    Page<Complaint> findByCommunity(Community community, Pageable pageable);

    long countByCommunity(Community community);
}
