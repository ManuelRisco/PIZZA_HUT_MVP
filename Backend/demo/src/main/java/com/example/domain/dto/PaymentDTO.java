package com.example.domain.dto;

import com.example.domain.model.Payment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDTO {
    private Integer id;
    private Integer orderId;
    private BigDecimal amount;
    private Integer paymentMethodId;
    private String status;
    private String transactionId;
    private LocalDateTime createdAt;

    // Constructor vacío
    public PaymentDTO() {}

    // Constructor desde entidad
    public PaymentDTO(Payment payment) {
        this.id = payment.getId();
        this.orderId = payment.getOrderId();
        this.amount = payment.getAmount();
        this.paymentMethodId = payment.getPaymentMethodId();
        this.status = payment.getStatus() != null ? payment.getStatus().name() : null;
        this.transactionId = payment.getTransactionId();
        this.createdAt = payment.getCreatedAt();
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Integer getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(Integer paymentMethodId) { this.paymentMethodId = paymentMethodId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
