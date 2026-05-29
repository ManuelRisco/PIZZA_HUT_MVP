package com.example.application.users.estructurales;

import com.example.application.users.comportamiento.UsuarioSpecification;
import com.example.domain.model.Usuario;
import java.util.ArrayList;
import java.util.List;

/**
 * Patrón Specification - Permite combinar múltiples especificaciones
 */
public class UsuarioSpecificationComposite implements UsuarioSpecification {
    
    private List<UsuarioSpecification> specifications;

    public UsuarioSpecificationComposite() {
        this.specifications = new ArrayList<>();
    }

    public void add(UsuarioSpecification spec) {
        this.specifications.add(spec);
    }

    @Override
    public boolean isSatisfiedBy(Usuario usuario) {
        // Todas las especificaciones deben cumplirse (AND lógico)
        return specifications.stream()
                .allMatch(spec -> spec.isSatisfiedBy(usuario));
    }

    public static UsuarioSpecificationComposite and(UsuarioSpecification... specs) {
        UsuarioSpecificationComposite composite = new UsuarioSpecificationComposite();
        for (UsuarioSpecification spec : specs) {
            composite.add(spec);
        }
        return composite;
    }
}
