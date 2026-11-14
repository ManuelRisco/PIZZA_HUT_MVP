package com.example.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockAlertDTO {

    private Integer id;
    private String itemType;
    private Integer itemId;
    private String itemName;
    private String unit;
    private String movementDirection;
    private String movementType;
    private BigDecimal previousStock;
    private BigDecimal movementQuantity;
    private BigDecimal newStock;
    private BigDecimal minStock;
    private BigDecimal deficitAmount;
    private String alertLevel;
    private String referenceType;
    private Integer referenceId;
    private String reason;
    private boolean resolved;
    private boolean emailSent;
    private String emailRecipients;
    private String emailError;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime resolvedAt;
    private BigDecimal resolvedByStock;

    public StockAlertDTO() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getMovementDirection() { return movementDirection; }
    public void setMovementDirection(String movementDirection) { this.movementDirection = movementDirection; }

    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }

    public BigDecimal getPreviousStock() { return previousStock; }
    public void setPreviousStock(BigDecimal previousStock) { this.previousStock = previousStock; }

    public BigDecimal getMovementQuantity() { return movementQuantity; }
    public void setMovementQuantity(BigDecimal movementQuantity) { this.movementQuantity = movementQuantity; }

    public BigDecimal getNewStock() { return newStock; }
    public void setNewStock(BigDecimal newStock) { this.newStock = newStock; }

    public BigDecimal getMinStock() { return minStock; }
    public void setMinStock(BigDecimal minStock) { this.minStock = minStock; }

    public BigDecimal getdeficitAmount() { return deficitAmount; }
    public BigDecimal getDeficitAmount() { return deficitAmount; }
    public void setDeficitAmount(BigDecimal deficitAmount) { this.deficitAmount = deficitAmount; }

    public String getAlertLevel() { return alertLevel; }
    public void setAlertLevel(String alertLevel) { this.alertLevel = alertLevel; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public Integer getReferenceId() { return referenceId; }
    public void setReferenceId(Integer referenceId) { this.referenceId = referenceId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }

    public boolean isEmailSent() { return emailSent; }
    public void setEmailSent(boolean emailSent) { this.emailSent = emailSent; }

    public String getEmailRecipients() { return emailRecipients; }
    public void setEmailRecipients(String emailRecipients) { this.emailRecipients = emailRecipients; }

    public String getEmailError() { return emailError; }
    public void setEmailError(String emailError) { this.emailError = emailError; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public BigDecimal getResolvedByStock() { return resolvedByStock; }
    public void setResolvedByStock(BigDecimal resolvedByStock) { this.resolvedByStock = resolvedByStock; }
}

