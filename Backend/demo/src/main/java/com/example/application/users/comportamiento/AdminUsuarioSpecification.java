package com.example.application.users.comportamiento;

import com.example.domain.model.Usuario;

/**
 * Patrón Specification - Filtra usuarios administradores
 */
public class AdminUsuarioSpecification implements UsuarioSpecification {
    
    @Override
    public boolean isSatisfiedBy(Usuario usuario) {
        return usuario.getRole() == Usuario.Role.ADMIN;
    }
}
