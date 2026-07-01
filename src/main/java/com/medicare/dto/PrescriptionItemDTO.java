package com.medicare.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PrescriptionItemDTO {
    private Long itemId;

    @NotNull
    private Long drugId;
    private String drugName;

    @NotNull @Min(1)
    private Integer quantity;
    private String dosageInstructions;
    private String duration;
    private String frequency;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Boolean isDispensed;
    private String remarks;
}


