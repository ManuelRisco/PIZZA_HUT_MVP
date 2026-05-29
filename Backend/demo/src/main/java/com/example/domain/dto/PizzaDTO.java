package com.example.domain.dto;

import com.example.domain.model.Pizza;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public class PizzaDTO {
    private Integer id;
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

    // Constructor vacío
    public PizzaDTO() {}

    // Constructor desde entidad
    public PizzaDTO(Pizza pizza) {
        this.id = pizza.getId();
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
    public boolean isAvailable() { return isAvailable; }
    @JsonProperty("isAvailable") 
    public void setAvailable(boolean available) { isAvailable = available; }

    @JsonProperty("isPopular")
    public boolean isPopular() { return isPopular; }
    @JsonProperty("isPopular")
    public void setPopular(boolean popular) { isPopular = popular; }

    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
}