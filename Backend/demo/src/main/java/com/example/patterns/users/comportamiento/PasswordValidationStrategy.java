package com.example.patterns.users.comportamiento;

import com.example.models.Usuario;

/**
 * Patr\u00f3n Strategy - Validaci\u00f3n de contrase\u00f1a
 */
public class PasswordValidationStrategy implements UsuarioValidationStrategy {
    
    private static final int MIN_LENGTH = 6;

    @Override
    public boolean validate(Usuario usuario) {
        return usuario.getPasswordHash() != null && 
               usuario.getPasswordHash().length() >= MIN_LENGTH;
    }

    @Override
    public String getErrorMessage() {
        return "La contrase\u00f1a debe tener al menos " + MIN_LENGTH + " caracteres";
    }
}
