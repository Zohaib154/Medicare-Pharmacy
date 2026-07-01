package com.medicare.repository;

import com.medicare.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByCnicNumber(String cnicNumber);

    Optional<Patient> findByContactNumber(String contactNumber);

    @Query("""
        SELECT p FROM Patient p
        WHERE p.isActive = true
        AND (LOWER(p.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
          OR p.contactNumber LIKE CONCAT('%', :q, '%')
          OR p.cnicNumber LIKE CONCAT('%', :q, '%'))
        """)
    Page<Patient> searchPatients(@Param("q") String query, Pageable pageable);

    Page<Patient> findByIsActiveTrue(Pageable pageable);

    long countByIsActiveTrue();
}


