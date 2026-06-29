package com.example.dtos;

import com.example.models.OrderTracking;
import java.time.LocalDateTime;

public class OrderTrackingDTO {
    private Integer id;
    private Integer orderId;
    private String status;
    private String description;
    private LocalDateTime createdAt;

    // Constructor vac\u00edo
    public OrderTrackingDTO() {}

    // Constructor desde entidad
    public OrderTrackingDTO(OrderTracking orderTracking) {
        this.id = orderTracking.getId();
        this.orderId = orderTracking.getOrderId();
        this.status = orderTracking.getStatus() != null ? orderTracking.getStatus().name() : null;
        this.description = orderTracking.getDescription();
        this.createdAt = orderTracking.getCreatedAt();
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
