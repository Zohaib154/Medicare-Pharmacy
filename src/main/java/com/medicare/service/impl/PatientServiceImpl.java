package com.medicare.service.impl;

import com.medicare.dto.PatientDTO;
import com.medicare.entity.Patient;
import com.medicare.exception.DuplicateResourceException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PatientServiceImpl {

    private final PatientRepository patientRepository;

    @Transactional
    public PatientDTO createPatient(PatientDTO dto) {
        if (dto.getCnicNumber() != null && !dto.getCnicNumber().isBlank()) {
            patientRepository.findByCnicNumber(dto.getCnicNumber()).ifPresent(p -> {
                throw new DuplicateResourceException("Patient already exists with CNIC: " + dto.getCnicNumber());
            });
        }
        Patient patient = mapToEntity(dto);
        Patient saved = patientRepository.save(patient);
        log.info("Patient registered: {} (ID: {})", saved.getFullName(), saved.getPatientId());
        return mapToDTO(saved);
    }

    @Transactional
    public PatientDTO updatePatient(Long id, PatientDTO dto) {
        Patient patient = patientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id));

        patient.setFullName(dto.getFullName());
        patient.setDateOfBirth(dto.getDateOfBirth());
        patient.setGender(Patient.Gender.valueOf(dto.getGender()));
        patient.setContactNumber(dto.getContactNumber());
        patient.setEmail(dto.getEmail());
        patient.setAddress(dto.getAddress());
        patient.setBloodGroup(dto.getBloodGroup());
        patient.setAllergies(dto.getAllergies());
        patient.setChronicConditions(dto.getChronicConditions());
        patient.setCurrentMedications(dto.getCurrentMedications());
        patient.setInsuranceProvider(dto.getInsuranceProvider());
        patient.setInsurancePolicyNo(dto.getInsurancePolicyNo());
        patient.setCnicNumber(dto.getCnicNumber());

        return mapToDTO(patientRepository.save(patient));
    }

    public PatientDTO getPatientById(Long id) {
        return patientRepository.findById(id)
            .map(this::mapToDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id));
    }

    public Page<PatientDTO> getAllPatients(Pageable pageable) {
        return patientRepository.findByIsActiveTrue(pageable).map(this::mapToDTO);
    }

    public Page<PatientDTO> searchPatients(String query, Pageable pageable) {
        return patientRepository.searchPatients(query, pageable).map(this::mapToDTO);
    }

    @Transactional
    public void deletePatient(Long id) {
        Patient patient = patientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id));
        patient.setIsActive(false);
        patientRepository.save(patient);
    }

    private Patient mapToEntity(PatientDTO dto) {
        return Patient.builder()
            .fullName(dto.getFullName())
            .dateOfBirth(dto.getDateOfBirth())
            .gender(Patient.Gender.valueOf(dto.getGender()))
            .contactNumber(dto.getContactNumber())
            .email(dto.getEmail())
            .address(dto.getAddress())
            .bloodGroup(dto.getBloodGroup())
            .allergies(dto.getAllergies())
            .chronicConditions(dto.getChronicConditions())
            .currentMedications(dto.getCurrentMedications())
            .cnicNumber(dto.getCnicNumber())
            .insuranceProvider(dto.getInsuranceProvider())
            .insurancePolicyNo(dto.getInsurancePolicyNo())
            .build();
    }

    public PatientDTO mapToDTO(Patient p) {
        PatientDTO dto = new PatientDTO();
        dto.setPatientId(p.getPatientId());
        dto.setFullName(p.getFullName());
        dto.setDateOfBirth(p.getDateOfBirth());
        dto.setAge(p.getDateOfBirth() != null
            ? Period.between(p.getDateOfBirth(), LocalDate.now()).getYears() : null);
        dto.setGender(p.getGender().name());
        dto.setContactNumber(p.getContactNumber());
        dto.setEmail(p.getEmail());
        dto.setAddress(p.getAddress());
        dto.setBloodGroup(p.getBloodGroup());
        dto.setAllergies(p.getAllergies());
        dto.setChronicConditions(p.getChronicConditions());
        dto.setCurrentMedications(p.getCurrentMedications());
        dto.setCnicNumber(p.getCnicNumber());
        dto.setInsuranceProvider(p.getInsuranceProvider());
        dto.setInsurancePolicyNo(p.getInsurancePolicyNo());
        dto.setIsActive(p.getIsActive());
        dto.setCreatedAt(p.getCreatedAt());
        return dto;
    }
}


