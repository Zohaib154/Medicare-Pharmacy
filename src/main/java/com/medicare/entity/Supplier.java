package com.medicare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "suppliers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Supplier extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long supplierId;

    @Column(nullable = false, length = 150)
    private String supplierName;

    @Column(length = 150)
    private String contactPerson;

    @Column(length = 15)
    private String contactNumber;

    @Column(length = 150)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String licenseNumber;       // Drug licence number

    @Column(length = 30)
    private String gstNumber;

    @Column(length = 50)
    private String bankName;

    @Column(length = 50)
    private String bankAccountNo;

    @Column(length = 20)
    private String paymentTerms;        // NET-30, NET-60, etc.

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal outstandingBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    private List<Inventory> supplyBatches;

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PurchaseOrder> purchaseOrders;
}


