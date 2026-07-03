package com.example.dtos;

public class PizzaIngredientDTO {
    private Integer pizzaId;
    private Integer ingredientId;
    private boolean isDefault;

    public PizzaIngredientDTO() {}

    public PizzaIngredientDTO(Integer pizzaId, Integer ingredientId, boolean isDefault) {
        this.pizzaId = pizzaId;
        this.ingredientId = ingredientId;
        this.isDefault = isDefault;
    }

    public Integer getPizzaId() {
        return pizzaId;
    }

    public void setPizzaId(Integer pizzaId) {
        this.pizzaId = pizzaId;
    }

    public Integer getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(Integer ingredientId) {
        this.ingredientId = ingredientId;
    }

    public boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
