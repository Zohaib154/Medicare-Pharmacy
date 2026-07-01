package com.medicare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "drugs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Drug extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long drugId;

    @Column(nullable = false, length = 200)
    private String drugName;

    @Column(length = 200)
    private String genericName;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 100)
    private String manufacturer;

    @Column(length = 50)
    private String dosageForm;          // Tablet, Capsule, Syrup, Injection, etc.

    @Column(length = 50)
    private String strength;            // e.g. 500mg, 10mg/5ml

    @Column(length = 20)
    private String scheduleType;        // OTC, Schedule H, Schedule H1, Schedule X

    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal mrp;

    @Column(length = 20)
    private String hsnCode;             // GST/Tax code

    @Column(precision = 5, scale = 2)
    private BigDecimal gstPercent;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String sideEffects;

    @Column(columnDefinition = "TEXT")
    private String contraindications;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "drug", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Inventory> inventoryBatches;

    @OneToMany(mappedBy = "drug", fetch = FetchType.LAZY)
    private List<PrescriptionItem> prescriptionItems;

    @OneToMany(mappedBy = "drug", fetch = FetchType.LAZY)
    private List<SaleItem> saleItems;
}


