package com.medicare.repository;

import com.medicare.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findByBillNumber(String billNumber);

    Page<Sale> findByPatient_PatientId(Long patientId, Pageable pageable);

    @Query("""
        SELECT s FROM Sale s
        WHERE s.saleDateTime BETWEEN :start AND :end
        ORDER BY s.saleDateTime DESC
        """)
    List<Sale> findSalesBetween(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s
        WHERE s.saleDateTime BETWEEN :start AND :end
        AND s.status = 'COMPLETED'
        """)
    BigDecimal sumRevenueBetween(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT COUNT(s) FROM Sale s
        WHERE s.saleDateTime BETWEEN :start AND :end
        AND s.status = 'COMPLETED'
        """)
    long countSalesBetween(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT COALESCE(AVG(s.totalAmount), 0) FROM Sale s
        WHERE s.saleDateTime BETWEEN :start AND :end
        AND s.status = 'COMPLETED'
        """)
    BigDecimal avgBillValueBetween(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT d.drugName, SUM(si.quantity) as totalQty, SUM(si.totalPrice) as revenue
        FROM SaleItem si JOIN si.drug d JOIN si.sale s
        WHERE s.saleDateTime BETWEEN :start AND :end
        AND s.status = 'COMPLETED'
        GROUP BY d.drugId, d.drugName
        ORDER BY revenue DESC
        """)
    List<Object[]> findTopSellingDrugs(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
    );

    @Query("""
        SELECT COALESCE(MAX(CAST(SUBSTRING(s.billNumber, 3) AS int)), 20000)
        FROM Sale s WHERE s.billNumber LIKE 'B-%'
        """)
    Optional<Integer> findMaxBillSuffix();
}


