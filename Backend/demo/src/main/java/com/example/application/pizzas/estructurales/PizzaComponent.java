package com.example.application.pizzas.estructurales;

import java.math.BigDecimal;

/**
 * Patrón Decorator - Componente base para pizzas con decoradores
 */
public interface PizzaComponent {
    String getDescription();
    BigDecimal getPrice();
}
