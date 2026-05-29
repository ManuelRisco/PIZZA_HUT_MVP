package com.example.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

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

    // Constructor vacío
    public PizzaCreateDTO() {}

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
    public boolean isAvailable() { return isAvailable; }
    @JsonProperty("isAvailable")
    public void setAvailable(boolean available) { isAvailable = available; }

    @JsonProperty("isPopular")
    public boolean isPopular() { return isPopular; }
    @JsonProperty("isPopular")
    public void setPopular(boolean popular) { isPopular = popular; }
}