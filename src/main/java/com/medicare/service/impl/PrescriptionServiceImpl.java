package com.medicare.service.impl;

import com.medicare.dto.PrescriptionDTO;
import com.medicare.dto.PrescriptionItemDTO;
import com.medicare.entity.*;
import com.medicare.entity.Prescription.PrescriptionStatus;
import com.medicare.exception.BusinessException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.*;
import com.medicare.service.DocumentNumberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PrescriptionServiceImpl {

    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final DrugRepository drugRepository;
    private final UserRepository userRepository;
    private final InventoryServiceImpl inventoryService;
    private final DocumentNumberService documentNumberService;

    @Transactional
    public PrescriptionDTO createPrescription(PrescriptionDTO dto) {
        Patient patient = patientRepository.findById(dto.getPatientId())
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + dto.getPatientId()));

        String rxNumber = documentNumberService.nextRxNumber();

        Prescription prescription = Prescription.builder()
            .rxNumber(rxNumber)
            .patient(patient)
            .doctorName(dto.getDoctorName())
            .doctorLicenseNo(dto.getDoctorLicenseNo())
            .hospitalClinic(dto.getHospitalClinic())
            .issueDate(dto.getIssueDate() != null ? dto.getIssueDate() : LocalDate.now())
            .diagnosis(dto.getDiagnosis())
            .notes(dto.getNotes())
            .discountPercent(dto.getDiscountPercent() != null ? dto.getDiscountPercent() : BigDecimal.ZERO)
            .status(PrescriptionStatus.PENDING)
            .items(new ArrayList<>())
            .build();

        // Build line items
        BigDecimal total = BigDecimal.ZERO;
        for (PrescriptionItemDTO itemDTO : dto.getItems()) {
            Drug drug = drugRepository.findById(itemDTO.getDrugId())
                .orElseThrow(() -> new ResourceNotFoundException("Drug not found: " + itemDTO.getDrugId()));

            BigDecimal unitPrice = itemDTO.getUnitPrice() != null ? itemDTO.getUnitPrice() :
                (drug.getUnitPrice() != null ? drug.getUnitPrice() : 
                (drug.getMrp() != null ? drug.getMrp() : BigDecimal.ZERO));
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemDTO.getQuantity()));

            PrescriptionItem item = PrescriptionItem.builder()
                .prescription(prescription)
                .drug(drug)
                .quantity(itemDTO.getQuantity())
                .dosageInstructions(itemDTO.getDosageInstructions())
                .duration(itemDTO.getDuration())
                .frequency(itemDTO.getFrequency())
                .unitPrice(unitPrice)
                .totalPrice(lineTotal)
                .build();

            prescription.getItems().add(item);
            total = total.add(lineTotal);
        }

        // Apply discount
        if (prescription.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = total.multiply(prescription.getDiscountPercent()).divide(BigDecimal.valueOf(100));
            total = total.subtract(discount);
        }
        prescription.setTotalAmount(total);

        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Prescription created: {} for patient: {}", rxNumber, patient.getFullName());
        return mapToDTO(saved);
    }

    @Transactional
    public PrescriptionDTO dispensePrescription(Long prescriptionId, Long pharmacistId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + prescriptionId));

        if (prescription.getStatus() == PrescriptionStatus.DISPENSED) {
            throw new BusinessException("Prescription already dispensed: " + prescription.getRxNumber());
        }
        if (prescription.getStatus() == PrescriptionStatus.CANCELLED) {
            throw new BusinessException("Cannot dispense a cancelled prescription");
        }

        User pharmacist = userRepository.findById(pharmacistId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + pharmacistId));

        for (PrescriptionItem item : prescription.getItems()) {
            inventoryService.deductStockFefo(item.getDrug().getDrugId(), item.getQuantity());
            item.setIsDispensed(true);
        }

        prescription.setStatus(PrescriptionStatus.DISPENSED);
        prescription.setDispensedDate(LocalDate.now());
        prescription.setDispensedBy(pharmacist);

        Prescription updated = prescriptionRepository.save(prescription);
        log.info("Prescription dispensed: {} by pharmacist: {}", prescription.getRxNumber(), pharmacist.getFullName());
        return mapToDTO(updated);
    }

    @Transactional
    public PrescriptionDTO cancelPrescription(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));

        if (prescription.getStatus() == PrescriptionStatus.DISPENSED) {
            throw new BusinessException("Cannot cancel a dispensed prescription");
        }

        prescription.setStatus(PrescriptionStatus.CANCELLED);
        return mapToDTO(prescriptionRepository.save(prescription));
    }

    public Page<PrescriptionDTO> getAllPrescriptions(Pageable pageable) {
        return prescriptionRepository.findAll(pageable).map(this::mapToDTO);
    }

    public Page<PrescriptionDTO> getPrescriptionsByStatus(PrescriptionStatus status, Pageable pageable) {
        return prescriptionRepository.findByStatus(status, pageable).map(this::mapToDTO);
    }

    public PrescriptionDTO getPrescriptionById(Long id) {
        return prescriptionRepository.findById(id)
            .map(this::mapToDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));
    }

    public List<PrescriptionDTO> getTodaysPrescriptions() {
        return prescriptionRepository.findTodaysPrescriptions(LocalDate.now())
            .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // ---- Mapping ----
    private PrescriptionDTO mapToDTO(Prescription p) {
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setPrescriptionId(p.getPrescriptionId());
        dto.setRxNumber(p.getRxNumber());
        dto.setPatientId(p.getPatient().getPatientId());
        dto.setPatientName(p.getPatient().getFullName());
        dto.setDoctorName(p.getDoctorName());
        dto.setDoctorLicenseNo(p.getDoctorLicenseNo());
        dto.setHospitalClinic(p.getHospitalClinic());
        dto.setIssueDate(p.getIssueDate());
        dto.setDispensedDate(p.getDispensedDate());
        dto.setDiagnosis(p.getDiagnosis());
        dto.setStatus(p.getStatus().name());
        dto.setNotes(p.getNotes());
        dto.setTotalAmount(p.getTotalAmount());
        dto.setDiscountPercent(p.getDiscountPercent());

        if (p.getDispensedBy() != null) {
            dto.setDispensedByName(p.getDispensedBy().getFullName());
        }

        if (p.getItems() != null) {
            dto.setItems(p.getItems().stream().map(item -> {
                PrescriptionItemDTO i = new PrescriptionItemDTO();
                i.setItemId(item.getItemId());
                i.setDrugId(item.getDrug().getDrugId());
                i.setDrugName(item.getDrug().getDrugName());
                i.setQuantity(item.getQuantity());
                i.setDosageInstructions(item.getDosageInstructions());
                i.setDuration(item.getDuration());
                i.setFrequency(item.getFrequency());
                i.setUnitPrice(item.getUnitPrice());
                i.setTotalPrice(item.getTotalPrice());
                i.setIsDispensed(item.getIsDispensed());
                return i;
            }).collect(Collectors.toList()));
        }

        return dto;
    }
}


