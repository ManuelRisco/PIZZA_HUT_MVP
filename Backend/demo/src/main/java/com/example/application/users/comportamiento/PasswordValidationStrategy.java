package com.example.application.users.comportamiento;

import com.example.domain.model.Usuario;

/**
 * Patrón Strategy - Validación de contraseña
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
        return "La contraseña debe tener al menos " + MIN_LENGTH + " caracteres";
    }
}
