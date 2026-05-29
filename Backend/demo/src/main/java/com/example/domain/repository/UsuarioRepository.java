package com.example.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.domain.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByEmail(String email);
    boolean existsByName(String name);
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByName(String name);
}
