package com.medicare.repository;

import com.medicare.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Page<Supplier> findByIsActiveTrue(Pageable pageable);

    @Query("""
        SELECT s FROM Supplier s
        WHERE s.isActive = true
        AND (LOWER(s.supplierName) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(s.city) LIKE LOWER(CONCAT('%', :q, '%'))
          OR s.contactNumber LIKE CONCAT('%', :q, '%'))
        """)
    Page<Supplier> searchSuppliers(@Param("q") String query, Pageable pageable);

    boolean existsBySupplierNameIgnoreCase(String supplierName);
}


