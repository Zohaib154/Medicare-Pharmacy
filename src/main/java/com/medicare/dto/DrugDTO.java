package com.medicare.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DrugDTO {
    private Long drugId;

    @NotBlank(message = "Drug name is required")
    @Size(max = 200)
    private String drugName;

    private String genericName;

    @NotBlank(message = "Category is required")
    private String category;

    private String manufacturer;
    private String dosageForm;
    private String strength;
    private String scheduleType;

    @DecimalMin("0.0")
    private BigDecimal unitPrice;

    @DecimalMin("0.0")
    private BigDecimal mrp;

    private String hsnCode;

    @DecimalMin("0.0") @DecimalMax("100.0")
    private BigDecimal gstPercent;

    private String description;
    private String sideEffects;
    private String contraindications;
    private Boolean isActive;
    private LocalDateTime createdAt;

    /** Populated by GET /drugs/with-stock only */
    private Integer totalStock;
}


