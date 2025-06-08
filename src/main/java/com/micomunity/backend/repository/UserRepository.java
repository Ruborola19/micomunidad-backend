package com.micomunity.backend.repository;

import com.micomunity.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByDni(String dni);
    boolean existsByEmail(String email);
    boolean existsByDni(String dni);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.community WHERE u.email = :email")
    Optional<User> findByEmailWithCommunity(@Param("email") String email);
} 