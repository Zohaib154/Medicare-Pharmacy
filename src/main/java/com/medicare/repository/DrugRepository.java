package com.medicare.repository;

import com.medicare.entity.Drug;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrugRepository extends JpaRepository<Drug, Long> {

    Optional<Drug> findByDrugNameIgnoreCase(String drugName);

    Page<Drug> findByIsActiveTrue(Pageable pageable);

    Page<Drug> findByCategoryIgnoreCaseAndIsActiveTrue(String category, Pageable pageable);

    @Query("""
        SELECT d FROM Drug d
        WHERE d.isActive = true
        AND (LOWER(d.drugName) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(d.genericName) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(d.category) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(d.manufacturer) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Drug> searchDrugs(@Param("search") String search, Pageable pageable);

    @Query("SELECT DISTINCT d.category FROM Drug d WHERE d.isActive = true ORDER BY d.category")
    List<String> findAllCategories();

    @Query("""
        SELECT d FROM Drug d
        WHERE d.scheduleType IN ('Schedule H', 'Schedule H1', 'Schedule X')
        AND d.isActive = true
        """)
    List<Drug> findControlledDrugs();

    boolean existsByDrugNameIgnoreCase(String drugName);

    List<Drug> findByIsActiveTrueOrderByDrugNameAsc();
}


