package com.example.application.users.comportamiento;

import com.example.domain.model.Usuario;

/**
 * Patrón Specification - Define criterios de búsqueda/filtrado
 */
public interface UsuarioSpecification {
    boolean isSatisfiedBy(Usuario usuario);
}
