package com.example.dtos;

import com.example.models.Extra;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExtraDTO {
    
    private Integer id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private Boolean isAvailable;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor vac\u00edo
    public ExtraDTO() {}

    // Constructor desde entidad
    public ExtraDTO(Extra extra) {
        this.id = extra.getId();
        this.name = extra.getName();
        this.description = extra.getDescription();
        this.price = extra.getPrice();
        this.category = extra.getCategory() != null ? extra.getCategory().name() : null;
        this.isAvailable = extra.getIsAvailable();
        this.displayOrder = extra.getDisplayOrder();
        this.createdAt = extra.getCreatedAt();
        this.updatedAt = extra.getUpdatedAt();
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
