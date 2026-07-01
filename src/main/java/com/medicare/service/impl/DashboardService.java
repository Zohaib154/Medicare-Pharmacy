package com.medicare.service.impl;

import com.medicare.dto.DashboardDTO;
import com.medicare.dto.TopDrugDTO;
import com.medicare.entity.Prescription.PrescriptionStatus;
import com.medicare.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final SaleRepository saleRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final InventoryRepository inventoryRepository;

    public DashboardDTO getDashboardMetrics() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(23, 59, 59);
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = LocalDate.now().atTime(23, 59, 59);
        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = LocalDate.now().minusDays(1).atTime(23, 59, 59);

        // Revenue
        BigDecimal todayRevenue = saleRepository.sumRevenueBetween(todayStart, todayEnd);
        BigDecimal yesterdayRevenue = saleRepository.sumRevenueBetween(yesterdayStart, yesterdayEnd);
        BigDecimal monthRevenue = saleRepository.sumRevenueBetween(monthStart, monthEnd);

        // Transactions
        long todayTransactions = saleRepository.countSalesBetween(todayStart, todayEnd);
        BigDecimal avgBillValue = saleRepository.avgBillValueBetween(monthStart, monthEnd);

        // Prescriptions
        long pendingRx = prescriptionRepository.countByStatus(PrescriptionStatus.PENDING);
        long todayRx = prescriptionRepository.countTodaysPrescriptions(LocalDate.now());

        // Patients
        long totalPatients = patientRepository.countByIsActiveTrue();

        // Stock
        long outOfStock = inventoryRepository.countOutOfStock();
        long expiringIn30Days = inventoryRepository.countExpiringWithin(
            LocalDate.now(), LocalDate.now().plusDays(30));

        // Top drugs
        List<Object[]> rawTop = saleRepository.findTopSellingDrugs(
            monthStart, monthEnd, PageRequest.of(0, 5));
        List<TopDrugDTO> topDrugs = new ArrayList<>();
        for (Object[] row : rawTop) {
            topDrugs.add(TopDrugDTO.builder()
                .drugName((String) row[0])
                .unitsSold(((Number) row[1]).intValue())
                .revenue((BigDecimal) row[2])
                .build());
        }

        return DashboardDTO.builder()
            .todayRevenue(todayRevenue)
            .yesterdayRevenue(yesterdayRevenue)
            .monthRevenue(monthRevenue)
            .todayTransactions(todayTransactions)
            .avgBillValue(avgBillValue)
            .pendingPrescriptions(pendingRx)
            .todayPrescriptions(todayRx)
            .totalPatients(totalPatients)
            .outOfStockCount(outOfStock)
            .expiringIn30Days(expiringIn30Days)
            .topSellingDrugs(topDrugs)
            .build();
    }

    public List<java.util.Map<String, Object>> getWeeklyRevenueData() {
        List<java.util.Map<String, Object>> data = new ArrayList<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("EEE");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(23, 59, 59);
            BigDecimal revenue = saleRepository.sumRevenueBetween(start, end);
            if (revenue == null) {
                revenue = BigDecimal.ZERO;
            }
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("day", date.format(formatter));
            map.put("revenue", revenue);
            data.add(map);
        }
        return data;
    }
}


