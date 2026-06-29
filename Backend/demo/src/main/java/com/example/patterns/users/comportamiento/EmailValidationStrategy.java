package com.example.patterns.users.comportamiento;

import com.example.models.Usuario;

/**
 * Patr\u00f3n Strategy - Validaci\u00f3n de email
 */
public class EmailValidationStrategy implements UsuarioValidationStrategy {
    
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";

    @Override
    public boolean validate(Usuario usuario) {
        return usuario.getEmail() != null && 
               usuario.getEmail().matches(EMAIL_REGEX);
    }

    @Override
    public String getErrorMessage() {
        return "El email no tiene un formato v\u00e1lido";
    }
}
