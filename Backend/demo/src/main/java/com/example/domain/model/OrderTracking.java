package com.example.domain.model;

import java.time.LocalDateTime;

public class OrderTracking {

    private Integer id;
    private Integer orderId;
    private Order.OrderStatus status;
    private String description;
    private LocalDateTime createdAt;

    // ==== Constructores ====
    public OrderTracking() {}

    public OrderTracking(Integer id, Integer orderId, Order.OrderStatus status,
                         String description, LocalDateTime createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.status = status;
        this.description = description;
        this.createdAt = createdAt;
    }

    // ==== Getters y Setters ====
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Order.OrderStatus getStatus() {
        return status;
    }

    public void setStatus(Order.OrderStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
