package com.medicare.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SaleItemDTO {
    private Long saleItemId;

    @NotNull
    private Long drugId;
    private String drugName;

    @NotNull @Min(1)
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private BigDecimal gstAmount;
    private BigDecimal totalPrice;
}


