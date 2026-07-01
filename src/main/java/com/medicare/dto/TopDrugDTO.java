package com.medicare.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TopDrugDTO {
    private String drugName;
    private Integer unitsSold;
    private BigDecimal revenue;
}


