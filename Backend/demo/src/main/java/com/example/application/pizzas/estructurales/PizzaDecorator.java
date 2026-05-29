package com.example.application.pizzas.estructurales;

import java.math.BigDecimal;

/**
 * Patrón Decorator - Decorador base abstracto para ingredientes extras
 */
public abstract class PizzaDecorator implements PizzaComponent {
    
    protected PizzaComponent pizza;

    public PizzaDecorator(PizzaComponent pizza) {
        this.pizza = pizza;
    }

    @Override
    public String getDescription() {
        return pizza.getDescription();
    }

    @Override
    public BigDecimal getPrice() {
        return pizza.getPrice();
    }
}
