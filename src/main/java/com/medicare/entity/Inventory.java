package com.medicare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "inventory")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Inventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inventoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_id", nullable = false)
    private Drug drug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false, length = 50)
    private String batchNumber;

    @Column(nullable = false)
    private Integer quantityInStock;

    @Builder.Default
    @Column(nullable = false)
    private Integer reorderLevel = 50;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private LocalDate manufacturingDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal sellingPrice;

    @Column(length = 50)
    private String storageLocation;     // Shelf/Rack number

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StockStatus stockStatus = StockStatus.IN_STOCK;

    public enum StockStatus {
        IN_STOCK, LOW_STOCK, OUT_OF_STOCK, EXPIRED, RECALLED
    }

    @PreUpdate
    @PrePersist
    public void updateStockStatus() {
        if (quantityInStock <= 0) {
            this.stockStatus = StockStatus.OUT_OF_STOCK;
        } else if (quantityInStock <= reorderLevel) {
            this.stockStatus = StockStatus.LOW_STOCK;
        } else if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            this.stockStatus = StockStatus.EXPIRED;
        } else {
            this.stockStatus = StockStatus.IN_STOCK;
        }
    }
}


