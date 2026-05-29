package com.example.application.pizzas.comportamiento;

import com.example.domain.model.Pizza;

/**
 * Patrón Specification - Define criterios de búsqueda/filtrado de pizzas
 */
public interface PizzaSpecification {
    boolean isSatisfiedBy(Pizza pizza);
}
