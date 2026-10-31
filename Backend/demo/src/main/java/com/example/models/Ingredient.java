package com.example.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ingredients")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal extraCost = BigDecimal.ZERO;

    @Column(name = "isAvailable", nullable = false)
    private boolean isAvailable = true;

    @Column(nullable = false, length = 20)
    private String unit = "g";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal stockQuantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minStock = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal extraPortionQuantity = new BigDecimal("50.00");

    @Column(nullable = false)
    private boolean allowAsExtra = true;

    @Column(nullable = false)
    @JsonIgnore // Ocultar metadatos internos
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @JsonIgnore // Ocultar metadatos internos
    private LocalDateTime updatedAt = LocalDateTime.now();

    @JsonIgnore // Ocultar metadatos internos
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "ingredient", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<PizzaIngredient> pizzaIngredients;

    // Constructor vacío
    public Ingredient() {
    }

    // Constructor completo original preservado por compatibilidad
    public Ingredient(Integer id, String name, BigDecimal extraCost, boolean isAvailable,
            LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt,
            List<PizzaIngredient> pizzaIngredients) {
        this.id = id;
        this.name = name;
        this.extraCost = extraCost;
        this.isAvailable = isAvailable;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.pizzaIngredients = pizzaIngredients;
    }

    // Getters y setters
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

    public BigDecimal getExtraCost() {
        return extraCost;
    }

    public void setExtraCost(BigDecimal extraCost) {
        this.extraCost = extraCost;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(BigDecimal stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public BigDecimal getMinStock() {
        return minStock;
    }

    public void setMinStock(BigDecimal minStock) {
        this.minStock = minStock;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getExtraPortionQuantity() {
        return extraPortionQuantity;
    }

    public void setExtraPortionQuantity(BigDecimal extraPortionQuantity) {
        this.extraPortionQuantity = extraPortionQuantity;
    }

    public boolean isAllowAsExtra() {
        return allowAsExtra;
    }

    public void setAllowAsExtra(boolean allowAsExtra) {
        this.allowAsExtra = allowAsExtra;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public List<PizzaIngredient> getPizzaIngredients() {
        return pizzaIngredients;
    }

    public void setPizzaIngredients(List<PizzaIngredient> pizzaIngredients) {
        this.pizzaIngredients = pizzaIngredients;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null)
            this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
