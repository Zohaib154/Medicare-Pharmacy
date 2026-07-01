package com.medicare.repository;

import com.medicare.entity.Prescription;
import com.medicare.entity.Prescription.PrescriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByRxNumber(String rxNumber);

    Page<Prescription> findByPatient_PatientId(Long patientId, Pageable pageable);

    Page<Prescription> findByStatus(PrescriptionStatus status, Pageable pageable);

    @Query("""
        SELECT p FROM Prescription p
        WHERE DATE(p.issueDate) = :today
        ORDER BY p.createdAt DESC
        """)
    List<Prescription> findTodaysPrescriptions(@Param("today") LocalDate today);

    long countByStatus(PrescriptionStatus status);

    @Query("""
        SELECT COUNT(p) FROM Prescription p
        WHERE DATE(p.issueDate) = :today
        """)
    long countTodaysPrescriptions(@Param("today") LocalDate today);

    @Query("""
        SELECT p FROM Prescription p
        WHERE p.patient.patientId = :patientId
        ORDER BY p.issueDate DESC
        """)
    List<Prescription> findRecentByPatient(@Param("patientId") Long patientId, Pageable pageable);

    @Query("""
        SELECT COALESCE(MAX(CAST(SUBSTRING(p.rxNumber, 4) AS int)), 10000)
        FROM Prescription p WHERE p.rxNumber LIKE 'RX-%'
        """)
    Optional<Integer> findMaxRxSuffix();
}


