package com.medicare.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SaleDTO {
    private Long saleId;
    private String billNumber;
    private Long patientId;
    private String patientName;
    private Long prescriptionId;
    private String rxNumber;

    @NotNull(message = "Sold-by user ID is required")
    private Long soldById;
    private String soldByName;

    private LocalDateTime saleDateTime;
    private String paymentMethod;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal changeReturned;
    private String notes;

    @NotEmpty(message = "Sale must have at least one item")
    private List<SaleItemDTO> items;
}


