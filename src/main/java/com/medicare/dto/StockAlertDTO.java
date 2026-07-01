package com.medicare.dto;

import lombok.*;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StockAlertDTO {
    private String drugName;
    private String batchNumber;
    private Integer currentStock;
    private Integer reorderLevel;
    private String status;
    private LocalDate expiryDate;
}


