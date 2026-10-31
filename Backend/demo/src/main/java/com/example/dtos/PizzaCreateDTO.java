package com.example.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public class PizzaCreateDTO {
    private Integer categoryId;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal price;

    @JsonProperty("isAvailable")
    private boolean isAvailable;

    @JsonProperty("isPopular")
    private boolean isPopular;

    private List<PizzaIngredientDTO> recipe;

    // Constructor vacío
    public PizzaCreateDTO() {
        // Constructor vacío para serialización
    }

    // Getters y setters
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    @JsonProperty("isAvailable")
    public boolean getIsAvailable() { return isAvailable; }
    @JsonProperty("isAvailable")
    public void setIsAvailable(boolean isAvailable) { this.isAvailable = isAvailable; }

    @JsonProperty("isPopular")
    public boolean getIsPopular() { return isPopular; }
    @JsonProperty("isPopular")
    public void setIsPopular(boolean isPopular) { this.isPopular = isPopular; }

    public List<PizzaIngredientDTO> getRecipe() { return recipe; }
    public void setRecipe(List<PizzaIngredientDTO> recipe) { this.recipe = recipe; }
}
