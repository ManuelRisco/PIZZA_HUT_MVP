package com.example.dtos;

import com.example.models.Promotion;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PromotionDTO {
    
    private Integer id;
    private String code;
    private String name;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal finalPrice;
    private BigDecimal minPurchase;
    private BigDecimal maxDiscount;
    private Boolean isActive;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private Integer usageCount;
    private String applicableTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Campos calculados
    private Boolean currentlyActive;
    private Integer remainingUses;

    // Constructor vac\u00edo
    public PromotionDTO() {}

    // Constructor desde entidad
    public PromotionDTO(Promotion promotion) {
        this.id = promotion.getId();
        this.code = promotion.getCode();
        this.name = promotion.getName();
        this.description = promotion.getDescription();
        this.discountType = promotion.getDiscountType() != null ? promotion.getDiscountType().name() : null;
        this.discountValue = promotion.getDiscountValue();
        this.finalPrice = promotion.getFinalPrice();
        this.minPurchase = promotion.getMinPurchase();
        this.maxDiscount = promotion.getMaxDiscount();
        this.isActive = promotion.getIsActive();
        this.startDate = promotion.getStartDate();
        this.endDate = promotion.getEndDate();
        this.usageLimit = promotion.getUsageLimit();
        this.usageCount = promotion.getUsageCount();
        this.applicableTo = promotion.getApplicableTo() != null ? promotion.getApplicableTo().name() : null;
        this.createdAt = promotion.getCreatedAt();
        this.updatedAt = promotion.getUpdatedAt();
        
        // Calcular campos derivados
        this.currentlyActive = promotion.isCurrentlyActive();
        this.remainingUses = promotion.getUsageLimit() != null ? 
            promotion.getUsageLimit() - promotion.getUsageCount() : null;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }

    public BigDecimal getMinPurchase() {
        return minPurchase;
    }

    public void setMinPurchase(BigDecimal minPurchase) {
        this.minPurchase = minPurchase;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(BigDecimal maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public String getApplicableTo() {
        return applicableTo;
    }

    public void setApplicableTo(String applicableTo) {
        this.applicableTo = applicableTo;
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

    public Boolean getCurrentlyActive() {
        return currentlyActive;
    }

    public void setCurrentlyActive(Boolean currentlyActive) {
        this.currentlyActive = currentlyActive;
    }

    public Integer getRemainingUses() {
        return remainingUses;
    }

    public void setRemainingUses(Integer remainingUses) {
        this.remainingUses = remainingUses;
    }
}
