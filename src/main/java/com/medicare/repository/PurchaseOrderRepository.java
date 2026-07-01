package com.medicare.repository;

import com.medicare.entity.PurchaseOrder;
import com.medicare.entity.PurchaseOrder.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("SELECT po FROM PurchaseOrder po "
        + "LEFT JOIN FETCH po.supplier "
        + "LEFT JOIN FETCH po.orderedBy "
        + "ORDER BY po.orderDate DESC")
    List<PurchaseOrder> findAllWithDetails();

    @Query("SELECT po FROM PurchaseOrder po "
        + "LEFT JOIN FETCH po.supplier "
        + "LEFT JOIN FETCH po.orderedBy "
        + "WHERE po.orderId = :id")
    Optional<PurchaseOrder> findSummaryById(@Param("id") Long id);

    @Query("SELECT po FROM PurchaseOrder po "
        + "LEFT JOIN FETCH po.items items "
        + "LEFT JOIN FETCH items.drug "
        + "LEFT JOIN FETCH po.supplier "
        + "LEFT JOIN FETCH po.orderedBy "
        + "WHERE po.orderId = :id")
    Optional<PurchaseOrder> findByIdWithDetails(@Param("id") Long id);

    Optional<PurchaseOrder> findByPoNumber(String poNumber);
    Page<PurchaseOrder> findByStatus(OrderStatus status, Pageable pageable);
    Page<PurchaseOrder> findBySupplier_SupplierId(Long supplierId, Pageable pageable);

    /** Returns all PO numbers so the service can compute the max suffix in Java — works on all databases */
    @Query("SELECT po.poNumber FROM PurchaseOrder po")
    List<String> findAllPoNumbers();
}


