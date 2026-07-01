package com.medicare.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardDTO {
    private BigDecimal todayRevenue;
    private BigDecimal yesterdayRevenue;
    private BigDecimal monthRevenue;
    private long todayTransactions;
    private BigDecimal avgBillValue;
    private long pendingPrescriptions;
    private long todayPrescriptions;
    private long totalPatients;
    private long outOfStockCount;
    private long expiringIn30Days;
    private List<TopDrugDTO> topSellingDrugs;
}


