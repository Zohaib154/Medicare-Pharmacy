package com.medicare.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PurchaseOrderDTO {
    private Long orderId;
    private String poNumber;

    @NotNull
    private Long supplierId;
    private String supplierName;

    @NotNull
    private Long orderedById;
    private String orderedByName;

    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String notes;

    @NotEmpty
    private List<PurchaseOrderItemDTO> items;
}


