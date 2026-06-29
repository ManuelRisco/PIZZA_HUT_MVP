package com.example.patterns.users.comportamiento;

import com.example.models.Usuario;

/**
 * Patr\u00f3n Strategy - Define el contrato para estrategias de validaci\u00f3n
 */
public interface UsuarioValidationStrategy {
    boolean validate(Usuario usuario);
    String getErrorMessage();
}
