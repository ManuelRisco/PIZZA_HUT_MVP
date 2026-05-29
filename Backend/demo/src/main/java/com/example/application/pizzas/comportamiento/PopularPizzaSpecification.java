package com.example.application.pizzas.comportamiento;

import com.example.domain.model.Pizza;

/**
 * Patrón Specification - Filtra pizzas populares
 */
public class PopularPizzaSpecification implements PizzaSpecification {
    
    @Override
    public boolean isSatisfiedBy(Pizza pizza) {
        return pizza.getIsPopular();
    }
}
