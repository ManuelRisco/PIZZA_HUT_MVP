package com.example.application.users.comportamiento;

import com.example.domain.model.Usuario;

/**
 * Patrón Specification - Filtra usuarios activos (no eliminados)
 */
public class ActiveUsuarioSpecification implements UsuarioSpecification {
    
    @Override
    public boolean isSatisfiedBy(Usuario usuario) {
        return usuario.getDeletedAt() == null;
    }
}
