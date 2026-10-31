package com.example.dtos;

import com.example.models.Pizza;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public class PizzaDTO {
    private Integer id;
    private Integer categoryId;
    private String categoryName;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal price;

    @JsonProperty("isAvailable")
    private boolean isAvailable;

    @JsonProperty("isPopular")
    private boolean isPopular;

    private List<String> ingredients;
    private List<PizzaIngredientDTO> recipe;
    private Integer availableStock;
    private BigDecimal productionCost;
    private BigDecimal profitMargin;
    private Integer recipeCount;
    private boolean lowStock;
    private boolean outOfStock;

    // Constructor vacío
    public PizzaDTO() {}

    // Constructor desde entidad
    public PizzaDTO(Pizza pizza) {
        this.id = pizza.getId();
        this.categoryId = pizza.getCategory() != null ? pizza.getCategory().getId() : null;
        this.categoryName = pizza.getCategory() != null ? pizza.getCategory().getName() : null;
        this.name = pizza.getName();
        this.description = pizza.getDescription();
        this.imageUrl = pizza.getImageUrl();
        this.price = pizza.getPrice();
        this.isAvailable = pizza.getIsAvailable();
        this.isPopular = pizza.getIsPopular();
    }

    // Getters y setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

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

    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }

    public List<PizzaIngredientDTO> getRecipe() { return recipe; }
    public void setRecipe(List<PizzaIngredientDTO> recipe) { this.recipe = recipe; }

    public Integer getAvailableStock() { return availableStock; }
    public void setAvailableStock(Integer availableStock) { this.availableStock = availableStock; }

    public BigDecimal getProductionCost() { return productionCost; }
    public void setProductionCost(BigDecimal productionCost) { this.productionCost = productionCost; }

    public BigDecimal getProfitMargin() { return profitMargin; }
    public void setProfitMargin(BigDecimal profitMargin) { this.profitMargin = profitMargin; }

    public Integer getRecipeCount() { return recipeCount; }
    public void setRecipeCount(Integer recipeCount) { this.recipeCount = recipeCount; }

    public boolean isLowStock() { return lowStock; }
    public void setLowStock(boolean lowStock) { this.lowStock = lowStock; }

    public boolean isOutOfStock() { return outOfStock; }
    public void setOutOfStock(boolean outOfStock) { this.outOfStock = outOfStock; }
}
