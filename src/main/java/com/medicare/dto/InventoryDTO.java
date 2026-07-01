package com.medicare.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryDTO {
    private Long inventoryId;

    @NotNull(message = "Drug ID is required")
    private Long drugId;
    private String drugName;

    private Long supplierId;
    private String supplierName;

    @NotBlank(message = "Batch number is required")
    private String batchNumber;

    @NotNull @Min(0)
    private Integer quantityInStock;

    private Integer reorderLevel;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;

    private LocalDate manufacturingDate;

    @DecimalMin("0.0")
    private BigDecimal purchasePrice;

    @DecimalMin("0.0")
    private BigDecimal sellingPrice;

    private String storageLocation;
    private String stockStatus;
}


