package com.example.dtos;

import com.example.models.Ingredient;
import com.example.models.PizzaIngredient;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PizzaIngredientDTO {
    private Integer pizzaId;
    private Integer ingredientId;
    private String ingredientName;
    private String unit;
    private BigDecimal unitCost;
    private BigDecimal stockQuantity;
    private boolean isDefault = true;
    private BigDecimal quantity;
    private BigDecimal lineCost;

    public PizzaIngredientDTO() {}

    public PizzaIngredientDTO(Integer pizzaId, Integer ingredientId, boolean isDefault) {
        this.pizzaId = pizzaId;
        this.ingredientId = ingredientId;
        this.isDefault = isDefault;
    }

    public PizzaIngredientDTO(PizzaIngredient pi) {
        this.pizzaId = pi.getId() != null ? pi.getId().getPizzaId() : (pi.getPizza() != null ? pi.getPizza().getId() : null);
        this.ingredientId = pi.getId() != null ? pi.getId().getIngredientId() : (pi.getIngredient() != null ? pi.getIngredient().getId() : null);
        this.isDefault = pi.getIsDefault();
        this.quantity = pi.getQuantity() != null ? pi.getQuantity() : new BigDecimal("50.00");
        Ingredient ing = pi.getIngredient();
        if (ing != null) {
            this.ingredientName = ing.getName();
            this.unit = ing.getUnit() != null ? ing.getUnit() : "g";
            this.unitCost = ing.getUnitCost() != null ? ing.getUnitCost() : BigDecimal.ZERO;
            this.stockQuantity = ing.getStockQuantity() != null ? ing.getStockQuantity() : BigDecimal.ZERO;
            this.lineCost = this.quantity.multiply(this.unitCost).setScale(2, RoundingMode.HALF_UP);
        } else {
            this.unitCost = BigDecimal.ZERO;
            this.stockQuantity = BigDecimal.ZERO;
            this.lineCost = BigDecimal.ZERO;
        }
    }

    public Integer getPizzaId() { return pizzaId; }
    public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }

    public Integer getIngredientId() { return ingredientId; }
    public void setIngredientId(Integer ingredientId) { this.ingredientId = ingredientId; }

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(BigDecimal stockQuantity) { this.stockQuantity = stockQuantity; }

    public boolean getIsDefault() { return isDefault; }
    public void setIsDefault(boolean isDefault) { this.isDefault = isDefault; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getLineCost() { return lineCost; }
    public void setLineCost(BigDecimal lineCost) { this.lineCost = lineCost; }
}
