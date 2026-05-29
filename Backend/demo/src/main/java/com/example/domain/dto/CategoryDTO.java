package com.example.domain.dto;

import java.time.LocalDateTime;

import com.example.domain.model.Category;

public class CategoryDTO {
    private Integer id;
    private String name;
    private String description;
    private String imageUrl;
    private Integer displayOrder;
    private LocalDateTime updatedAt;

    // Constructor vacío
    public CategoryDTO() {}

    // Constructor desde entidad
    public CategoryDTO(Category category) {
        this.id = category.getId();
        this.name = category.getName();
        this.description = category.getDescription();
        this.imageUrl = category.getImageUrl();
        this.displayOrder = category.getDisplayOrder();
        this.updatedAt = category.getUpdatedAt();
    }

    // Getters y setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}