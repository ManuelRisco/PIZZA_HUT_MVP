package com.example.infrastructure.persistence.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.example.domain.model.Payment;

@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private Integer orderId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    private Integer paymentMethodId;  // Cambio: FK a payment_methods

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Payment.PaymentStatus status = Payment.PaymentStatus.PENDING;

    @Column(length = 255)
    private String transactionId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ==== Ciclo de vida ====
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==== Constructores ====
    public PaymentEntity() {}

    // ==== Conversión entre dominio y entidad ====
    public static PaymentEntity fromDomain(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.id = payment.getId();
        entity.orderId = payment.getOrderId();
        entity.amount = payment.getAmount();
        entity.paymentMethodId = payment.getPaymentMethodId();
        entity.status = payment.getStatus();
        entity.transactionId = payment.getTransactionId();
        entity.createdAt = payment.getCreatedAt();
        entity.updatedAt = payment.getUpdatedAt();
        return entity;
    }

    public Payment toDomain() {
        return new Payment(
            this.id,
            this.orderId,
            this.amount,
            this.paymentMethodId,
            this.status,
            this.transactionId,
            this.createdAt,
            this.updatedAt
        );
    }

    // ==== Getters y Setters ====
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Integer getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(Integer paymentMethodId) { this.paymentMethodId = paymentMethodId; }

    public Payment.PaymentStatus getStatus() { return status; }
    public void setStatus(Payment.PaymentStatus status) { this.status = status; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
