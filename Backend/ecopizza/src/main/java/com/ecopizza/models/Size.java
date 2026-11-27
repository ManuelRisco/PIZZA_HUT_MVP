package com.ecopizza.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sizes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Size {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private BigDecimal extraCost;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal recipeMultiplier = BigDecimal.ONE;
    @Column(nullable = false)
    private Integer stockQuantity = 0;
    @Column(nullable = false)
    private Integer minStock = 0;
    private String description;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public Size(Integer id, String name, BigDecimal extraCost, String description,
            Integer displayOrder, LocalDateTime createdAt, LocalDateTime updatedAt,
            LocalDateTime deletedAt) {
        this.id = id;
        this.name = name;
        this.extraCost = extraCost;
        this.description = description;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public Integer getStockQuantity() {
        return stockQuantity != null ? stockQuantity : 0;
    }

    public Integer getMinStock() {
        return minStock != null ? minStock : 0;
    }

    @PrePersist
    protected void onCreate() {
        if (recipeMultiplier == null || recipeMultiplier.compareTo(BigDecimal.ZERO) <= 0) {
            recipeMultiplier = BigDecimal.ONE;
        }
        if (displayOrder == null) {
            displayOrder = 1;
        }
        if (extraCost == null) {
            extraCost = BigDecimal.ZERO;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (recipeMultiplier == null || recipeMultiplier.compareTo(BigDecimal.ZERO) <= 0) {
            recipeMultiplier = BigDecimal.ONE;
        }
        updatedAt = LocalDateTime.now();
    }
}
