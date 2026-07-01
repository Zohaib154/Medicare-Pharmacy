package com.medicare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "prescription_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PrescriptionItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_id", nullable = false)
    private Drug drug;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 200)
    private String dosageInstructions;  // e.g. "1 tablet twice daily after meals"

    @Column(length = 50)
    private String duration;            // e.g. "7 days"

    @Column(length = 200)
    private String frequency;           // e.g. "BD (Twice Daily)"

    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Builder.Default
    private Boolean isDispensed = false;

    @Column(columnDefinition = "TEXT")
    private String remarks;
}


