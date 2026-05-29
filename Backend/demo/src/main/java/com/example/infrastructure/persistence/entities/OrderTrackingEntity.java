package com.example.infrastructure.persistence.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.example.domain.model.OrderTracking;
import com.example.domain.model.Order;

@Entity
@Table(name = "order_tracking")
public class OrderTrackingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Order.OrderStatus status;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // ==== Ciclo de vida ====
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ==== Constructores ====
    public OrderTrackingEntity() {}

    // ==== Conversión entre dominio y entidad ====
    public static OrderTrackingEntity fromDomain(OrderTracking orderTracking) {
        OrderTrackingEntity entity = new OrderTrackingEntity();
        entity.id = orderTracking.getId();
        entity.orderId = orderTracking.getOrderId();
        entity.status = orderTracking.getStatus();
        entity.description = orderTracking.getDescription();
        entity.createdAt = orderTracking.getCreatedAt();
        return entity;
    }

    public OrderTracking toDomain() {
        return new OrderTracking(
            this.id,
            this.orderId,
            this.status,
            this.description,
            this.createdAt
        );
    }

    // ==== Getters y Setters ====
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public Order.OrderStatus getStatus() { return status; }
    public void setStatus(Order.OrderStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
