package com.example.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.models.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByEmail(String email);
    boolean existsByName(String name);
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByName(String name);
    List<Usuario> findByRoleAndDeletedAtIsNull(Usuario.Role role);
}
