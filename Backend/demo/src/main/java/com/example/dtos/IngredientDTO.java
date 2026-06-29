package com.example.dtos;

import java.math.BigDecimal;

import com.example.models.Ingredient;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IngredientDTO {
    private Integer id;
    private String name;
    private BigDecimal extraCost;
    
    @JsonProperty("isAvailable")
    private boolean isAvailable;

    // Constructor vac\u00edo
    public IngredientDTO() {}

    // Constructor desde entidad
    public IngredientDTO(Ingredient ingredient) {
        this.id = ingredient.getId();
        this.name = ingredient.getName();
        this.extraCost = ingredient.getExtraCost();
        this.isAvailable = ingredient.isAvailable();
    }

    // Getters y setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getExtraCost() { return extraCost; }
    public void setExtraCost(BigDecimal extraCost) { this.extraCost = extraCost; }

    @JsonProperty("isAvailable")
    public boolean getIsAvailable() { return isAvailable; }
    @JsonProperty("isAvailable")
    public void setIsAvailable(boolean isAvailable) { this.isAvailable = isAvailable; }
}
