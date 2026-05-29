package com.example.application.pizzas.comportamiento;

import com.example.domain.model.Pizza;

/**
 * Patrón Specification - Filtra pizzas disponibles
 */
public class AvailablePizzaSpecification implements PizzaSpecification {
    
    @Override
    public boolean isSatisfiedBy(Pizza pizza) {
        return pizza.getIsAvailable() && pizza.getDeletedAt() == null;
    }
}
