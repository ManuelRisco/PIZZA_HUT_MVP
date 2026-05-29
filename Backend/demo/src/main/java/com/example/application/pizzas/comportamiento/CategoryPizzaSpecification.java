package com.example.application.pizzas.comportamiento;

import com.example.domain.model.Pizza;

/**
 * Patrón Specification - Filtra pizzas por categoría
 */
public class CategoryPizzaSpecification implements PizzaSpecification {
    
    private final Integer categoryId;

    public CategoryPizzaSpecification(Integer categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public boolean isSatisfiedBy(Pizza pizza) {
        return pizza.getCategory() != null && 
               pizza.getCategory().getId().equals(categoryId);
    }
}
