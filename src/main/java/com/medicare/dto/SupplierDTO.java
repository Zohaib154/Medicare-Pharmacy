package com.medicare.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierDTO {
    private Long supplierId;

    @NotBlank(message = "Supplier name is required")
    private String supplierName;
    private String contactPerson;
    private String contactNumber;

    @Email
    private String email;
    private String address;
    private String city;
    private String licenseNumber;
    private String gstNumber;
    private String bankName;
    private String bankAccountNo;
    private String paymentTerms;
    private BigDecimal outstandingBalance;
    private Boolean isActive;
    private String notes;
}


