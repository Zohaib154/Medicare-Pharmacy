package com.medicare.repository;

import com.medicare.entity.Inventory;
import com.medicare.entity.Inventory.StockStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findByDrug_DrugIdOrderByExpiryDateAsc(Long drugId);

    Optional<Inventory> findByBatchNumber(String batchNumber);

    @Query("""
        SELECT i FROM Inventory i JOIN i.drug d
        WHERE i.quantityInStock <= i.reorderLevel
        AND i.stockStatus != 'EXPIRED'
        ORDER BY i.quantityInStock ASC
        """)
    List<Inventory> findLowStockItems();

    @Query("""
        SELECT i FROM Inventory i
        WHERE i.expiryDate BETWEEN :today AND :thresholdDate
        AND i.quantityInStock > 0
        ORDER BY i.expiryDate ASC
        """)
    List<Inventory> findExpiringBatches(
        @Param("today") LocalDate today,
        @Param("thresholdDate") LocalDate thresholdDate
    );

    @Query("""
        SELECT i FROM Inventory i
        WHERE i.expiryDate < :today
        AND i.stockStatus != 'EXPIRED'
        """)
    List<Inventory> findExpiredBatches(@Param("today") LocalDate today);

    @Query("""
        SELECT SUM(i.quantityInStock) FROM Inventory i
        WHERE i.drug.drugId = :drugId
        AND i.stockStatus NOT IN ('EXPIRED', 'RECALLED')
        """)
    Optional<Integer> getTotalStockForDrug(@Param("drugId") Long drugId);

    @Modifying
    @Query("""
        UPDATE Inventory i SET i.quantityInStock = i.quantityInStock - :qty,
        i.updatedAt = CURRENT_TIMESTAMP
        WHERE i.inventoryId = :invId AND i.quantityInStock >= :qty
        """)
    int deductStock(@Param("invId") Long invId, @Param("qty") int qty);

    Page<Inventory> findByStockStatus(StockStatus status, Pageable pageable);

    @Query("""
        SELECT COUNT(i) FROM Inventory i WHERE i.stockStatus = 'OUT_OF_STOCK'
        """)
    long countOutOfStock();

    @Query("""
        SELECT COUNT(i) FROM Inventory i
        WHERE i.expiryDate BETWEEN :today AND :threshold
        AND i.quantityInStock > 0
        """)
    long countExpiringWithin(@Param("today") LocalDate today, @Param("threshold") LocalDate threshold);
}


