package com.example.application.pizzas.creacionales;

import com.example.domain.model.Pizza;
import com.example.domain.model.Category;
import java.math.BigDecimal;

/**
 * Patrón Builder - Facilita la creación de pizzas personalizadas
 */
public class PizzaBuilder {
    private Pizza pizza;

    public PizzaBuilder() {
        this.pizza = new Pizza();
        this.pizza.setIsAvailable(true);
        this.pizza.setIsPopular(false);
    }

    public static PizzaBuilder builder() {
        return new PizzaBuilder();
    }

    public PizzaBuilder withName(String name) {
        this.pizza.setName(name);
        return this;
    }

    public PizzaBuilder withDescription(String description) {
        this.pizza.setDescription(description);
        return this;
    }

    public PizzaBuilder withCategory(Category category) {
        this.pizza.setCategory(category);
        return this;
    }

    public PizzaBuilder withPrice(BigDecimal price) {
        this.pizza.setPrice(price);
        return this;
    }

    public PizzaBuilder withImageUrl(String imageUrl) {
        this.pizza.setImageUrl(imageUrl);
        return this;
    }

    public PizzaBuilder available(boolean isAvailable) {
        this.pizza.setIsAvailable(isAvailable);
        return this;
    }

    public PizzaBuilder popular(boolean isPopular) {
        this.pizza.setIsPopular(isPopular);
        return this;
    }

    public Pizza build() {
        if (pizza.getName() == null || pizza.getName().isEmpty()) {
            throw new IllegalStateException("La pizza debe tener un nombre");
        }
        if (pizza.getPrice() == null || pizza.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("La pizza debe tener un precio válido");
        }
        return pizza;
    }
}
