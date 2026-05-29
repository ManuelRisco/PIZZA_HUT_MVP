package com.example.application.users.comportamiento;

import com.example.domain.model.Usuario;

/**
 * Patrón Strategy - Define el contrato para estrategias de validación
 */
public interface UsuarioValidationStrategy {
    boolean validate(Usuario usuario);
    String getErrorMessage();
}
