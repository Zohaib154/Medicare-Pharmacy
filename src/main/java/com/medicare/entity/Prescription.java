package com.medicare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "prescriptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Prescription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long prescriptionId;

    @Column(unique = true, nullable = false, length = 20)
    private String rxNumber;            // e.g. RX-10482

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispensed_by")
    private User dispensedBy;           // Pharmacist user

    @Column(nullable = false, length = 100)
    private String doctorName;

    @Column(length = 100)
    private String doctorLicenseNo;

    @Column(length = 150)
    private String hospitalClinic;

    @Column(nullable = false)
    private LocalDate issueDate;

    private LocalDate dispensedDate;

    @Column(length = 50)
    private String diagnosis;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PrescriptionStatus status = PrescriptionStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionItem> items;

    public enum PrescriptionStatus {
        PENDING, PROCESSING, DISPENSED, PARTIALLY_DISPENSED, CANCELLED, EXPIRED
    }
}


