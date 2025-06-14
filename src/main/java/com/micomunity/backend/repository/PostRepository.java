package com.micomunity.backend.repository;

import com.micomunity.backend.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByCommunityId(Long communityId, Pageable pageable);
    List<Post> findByAuthorIdOrderByCreationDateDesc(Long authorId);
}
