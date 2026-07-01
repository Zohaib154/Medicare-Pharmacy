package com.medicare.service.impl;

import com.medicare.dto.DrugDTO;
import com.medicare.entity.Drug;
import com.medicare.exception.BusinessException;
import com.medicare.exception.DuplicateResourceException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.DrugRepository;
import com.medicare.repository.InventoryRepository;
import com.medicare.service.DrugService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DrugServiceImpl implements DrugService {

    private final DrugRepository drugRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public DrugDTO createDrug(DrugDTO dto) {
        if (drugRepository.existsByDrugNameIgnoreCase(dto.getDrugName())) {
            throw new DuplicateResourceException("Drug already exists: " + dto.getDrugName());
        }
        Drug drug = mapToEntity(dto);
        Drug saved = drugRepository.save(drug);
        log.info("Drug created: {} (ID: {})", saved.getDrugName(), saved.getDrugId());
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public DrugDTO updateDrug(Long id, DrugDTO dto) {
        Drug drug = drugRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Drug not found with id: " + id));

        drug.setDrugName(dto.getDrugName());
        drug.setGenericName(dto.getGenericName());
        drug.setCategory(dto.getCategory());
        drug.setManufacturer(dto.getManufacturer());
        drug.setDosageForm(dto.getDosageForm());
        drug.setStrength(dto.getStrength());
        drug.setScheduleType(dto.getScheduleType());
        BigDecimal unitPrice = dto.getUnitPrice() != null ? dto.getUnitPrice() : dto.getMrp();
        drug.setUnitPrice(unitPrice);
        drug.setMrp(dto.getMrp());
        drug.setGstPercent(dto.getGstPercent());
        drug.setDescription(dto.getDescription());
        drug.setSideEffects(dto.getSideEffects());
        drug.setContraindications(dto.getContraindications());
        if (dto.getIsActive() != null) {
            drug.setIsActive(dto.getIsActive());
        }

        Drug updated = drugRepository.save(drug);
        log.info("Drug updated: {}", updated.getDrugId());
        return mapToDTO(updated);
    }

    @Override
    public DrugDTO getDrugById(Long id) {
        return drugRepository.findById(id)
            .map(this::mapToDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Drug not found with id: " + id));
    }

    @Override
    public Page<DrugDTO> getAllDrugs(Pageable pageable) {
        return drugRepository.findByIsActiveTrue(pageable).map(this::mapToDTO);
    }

    @Override
    public Page<DrugDTO> getAllDrugsIncludingInactive(Pageable pageable) {
        return drugRepository.findAll(pageable).map(this::mapToDTO);
    }

    @Override
    public Page<DrugDTO> searchDrugs(String query, Pageable pageable) {
        return drugRepository.searchDrugs(query, pageable).map(this::mapToDTO);
    }

    @Override
    public Page<DrugDTO> getDrugsByCategory(String category, Pageable pageable) {
        return drugRepository.findByCategoryIgnoreCaseAndIsActiveTrue(category, pageable).map(this::mapToDTO);
    }

    @Override
    @Transactional
    public void deleteDrug(Long id) {
        Drug drug = drugRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Drug not found with id: " + id));
        drug.setIsActive(false);        // Soft delete
        drugRepository.save(drug);
        log.info("Drug soft-deleted: {}", id);
    }

    @Override
    public List<String> getAllCategories() {
        return drugRepository.findAllCategories();
    }

    @Override
    public List<DrugDTO> getDrugsWithStock() {
        return drugRepository.findByIsActiveTrueOrderByDrugNameAsc().stream()
            .map(drug -> {
                DrugDTO dto = mapToDTO(drug);
                dto.setTotalStock(inventoryRepository.getTotalStockForDrug(drug.getDrugId()).orElse(0));
                return dto;
            })
            .collect(Collectors.toList());
    }
    // ---- Mapping helpers ----
    private Drug mapToEntity(DrugDTO dto) {
        BigDecimal unitPrice = dto.getUnitPrice() != null ? dto.getUnitPrice() : dto.getMrp();
        return Drug.builder()
            .drugName(dto.getDrugName())
            .genericName(dto.getGenericName())
            .category(dto.getCategory())
            .manufacturer(dto.getManufacturer())
            .dosageForm(dto.getDosageForm())
            .strength(dto.getStrength())
            .scheduleType(dto.getScheduleType())
            .unitPrice(unitPrice)
            .mrp(dto.getMrp())
            .hsnCode(dto.getHsnCode())
            .gstPercent(dto.getGstPercent())
            .description(dto.getDescription())
            .sideEffects(dto.getSideEffects())
            .contraindications(dto.getContraindications())
            .build();
    }

    public DrugDTO mapToDTO(Drug drug) {
        DrugDTO dto = new DrugDTO();
        dto.setDrugId(drug.getDrugId());
        dto.setDrugName(drug.getDrugName());
        dto.setGenericName(drug.getGenericName());
        dto.setCategory(drug.getCategory());
        dto.setManufacturer(drug.getManufacturer());
        dto.setDosageForm(drug.getDosageForm());
        dto.setStrength(drug.getStrength());
        dto.setScheduleType(drug.getScheduleType());
        dto.setUnitPrice(drug.getUnitPrice());
        dto.setMrp(drug.getMrp());
        dto.setHsnCode(drug.getHsnCode());
        dto.setGstPercent(drug.getGstPercent());
        dto.setDescription(drug.getDescription());
        dto.setSideEffects(drug.getSideEffects());
        dto.setContraindications(drug.getContraindications());
        dto.setIsActive(drug.getIsActive());
        dto.setCreatedAt(drug.getCreatedAt());
        return dto;
    }
}


