package com.ticketsystem.repository;

import com.ticketsystem.model.User;
import com.ticketsystem.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    // Aufgabe 15 / 25: Aktive Agenten nach Rolle (und Admins für Eskalation)
    List<User> findByRoleAndIsActiveTrue(UserRole role);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}