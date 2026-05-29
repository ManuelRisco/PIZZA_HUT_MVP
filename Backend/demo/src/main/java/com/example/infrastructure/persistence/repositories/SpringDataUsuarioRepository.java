package com.example.infrastructure.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.infrastructure.persistence.entities.UsuarioEntity;
import java.util.Optional;

public interface SpringDataUsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    Optional<UsuarioEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}