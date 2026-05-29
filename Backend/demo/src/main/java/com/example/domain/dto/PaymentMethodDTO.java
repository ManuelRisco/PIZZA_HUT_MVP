package com.example.domain.dto;

import com.example.domain.model.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class PaymentMethodDTO {
    private Integer id;
    private String name;
    private String description;
    
    @JsonProperty("isActive")
    private boolean isActive;
    private int displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor vacío
    public PaymentMethodDTO() {}

    // Constructor desde modelo de dominio
    public PaymentMethodDTO(PaymentMethod paymentMethod) {
        this.id = paymentMethod.getId();
        this.name = paymentMethod.getName();
        this.description = paymentMethod.getDescription();
        this.isActive = paymentMethod.isActive();
        this.displayOrder = paymentMethod.getDisplayOrder();
        this.createdAt = paymentMethod.getCreatedAt();
        this.updatedAt = paymentMethod.getUpdatedAt();
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @JsonProperty("isActive")
    public boolean isActive() { return isActive; }
    
    @JsonProperty("isActive")
    public void setActive(boolean active) { isActive = active; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
