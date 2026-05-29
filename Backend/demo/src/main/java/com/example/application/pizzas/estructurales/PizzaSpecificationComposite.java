package com.example.application.pizzas.estructurales;

import com.example.application.pizzas.comportamiento.PizzaSpecification;
import com.example.domain.model.Pizza;
import java.util.ArrayList;
import java.util.List;

/**
 * Patrón Composite - Combina múltiples especificaciones de pizzas
 */
public class PizzaSpecificationComposite implements PizzaSpecification {
    
    private List<PizzaSpecification> specifications;

    public PizzaSpecificationComposite() {
        this.specifications = new ArrayList<>();
    }

    public void add(PizzaSpecification spec) {
        this.specifications.add(spec);
    }

    @Override
    public boolean isSatisfiedBy(Pizza pizza) {
        // Todas las especificaciones deben cumplirse (AND lógico)
        return specifications.stream()
                .allMatch(spec -> spec.isSatisfiedBy(pizza));
    }

    /**
     * Método estático para combinar especificaciones fácilmente
     */
    public static PizzaSpecificationComposite and(PizzaSpecification... specs) {
        PizzaSpecificationComposite composite = new PizzaSpecificationComposite();
        for (PizzaSpecification spec : specs) {
            composite.add(spec);
        }
        return composite;
    }
}
