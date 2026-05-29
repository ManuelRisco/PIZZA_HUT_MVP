package com.example.application.users.comportamiento;

import com.example.domain.model.Usuario;
import java.util.ArrayList;
import java.util.List;

/**
 * Validador que usa las estrategias de validación
 */
public class UsuarioValidator {
    
    private List<UsuarioValidationStrategy> strategies;

    public UsuarioValidator() {
        this.strategies = new ArrayList<>();
    }

    public void addStrategy(UsuarioValidationStrategy strategy) {
        this.strategies.add(strategy);
    }

    public ValidationResult validate(Usuario usuario) {
        List<String> errors = new ArrayList<>();
        
        for (UsuarioValidationStrategy strategy : strategies) {
            if (!strategy.validate(usuario)) {
                errors.add(strategy.getErrorMessage());
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
