package com.example.application.pizzas.estructurales;

import com.example.domain.model.Pizza;
import java.math.BigDecimal;

/**
 * Patrón Decorator - Pizza base (componente concreto)
 */
public class BasicPizza implements PizzaComponent {
    
    private final Pizza pizza;

    public BasicPizza(Pizza pizza) {
        this.pizza = pizza;
    }

    @Override
    public String getDescription() {
        return pizza.getName();
    }

    @Override
    public BigDecimal getPrice() {
        return pizza.getPrice();
    }

    public Pizza getPizza() {
        return pizza;
    }
}
