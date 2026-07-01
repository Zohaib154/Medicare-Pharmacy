package com.medicare.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PurchaseOrderItemDTO {
    private Long poItemId;
    private Long drugId;
    private String drugName;
    private Integer orderedQuantity;
    private Integer receivedQuantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}


