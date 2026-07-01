package com.medicare.service.impl;

import com.medicare.dto.InventoryDTO;
import com.medicare.dto.StockAlertDTO;
import com.medicare.entity.Drug;
import com.medicare.entity.Inventory;
import com.medicare.entity.Inventory.StockStatus;
import com.medicare.entity.Supplier;
import com.medicare.exception.InsufficientStockException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.DrugRepository;
import com.medicare.repository.InventoryRepository;
import com.medicare.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medicare.service.StockDeduction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InventoryServiceImpl {

    private final InventoryRepository inventoryRepository;
    private final DrugRepository drugRepository;
    private final SupplierRepository supplierRepository;

    @Transactional
    public InventoryDTO addStock(InventoryDTO dto) {
        Drug drug = drugRepository.findById(dto.getDrugId())
            .orElseThrow(() -> new ResourceNotFoundException("Drug not found: " + dto.getDrugId()));

        Supplier supplier = null;
        if (dto.getSupplierId() != null) {
            supplier = supplierRepository.findById(dto.getSupplierId()).orElse(null);
        }

        Inventory inventory = Inventory.builder()
            .drug(drug)
            .supplier(supplier)
            .batchNumber(dto.getBatchNumber())
            .quantityInStock(dto.getQuantityInStock())
            .reorderLevel(dto.getReorderLevel() != null ? dto.getReorderLevel() : 50)
            .expiryDate(dto.getExpiryDate())
            .manufacturingDate(dto.getManufacturingDate())
            .purchasePrice(dto.getPurchasePrice())
            .sellingPrice(dto.getSellingPrice())
            .storageLocation(dto.getStorageLocation())
            .build();

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Stock added: {} units of Drug ID {} (Batch: {})",
            dto.getQuantityInStock(), dto.getDrugId(), dto.getBatchNumber());
        return mapToDTO(saved);
    }

    @Transactional
    public void deductStock(Long inventoryId, int quantity) {
        int updated = inventoryRepository.deductStock(inventoryId, quantity);
        if (updated == 0) {
            throw new InsufficientStockException("Insufficient stock in batch ID: " + inventoryId);
        }
    }

    /**
     * Deduct quantity across inventory batches using FEFO (first-expire-first-out).
     */
    @Transactional
    public List<StockDeduction> deductStockFefo(Long drugId, int quantity) {
        List<Inventory> batches = inventoryRepository.findByDrug_DrugIdOrderByExpiryDateAsc(drugId)
            .stream()
            .filter(i -> i.getStockStatus() != StockStatus.EXPIRED && i.getQuantityInStock() > 0)
            .collect(Collectors.toList());

        int remaining = quantity;
        List<StockDeduction> deductions = new ArrayList<>();
        for (Inventory batch : batches) {
            if (remaining <= 0) {
                break;
            }
            int deduct = Math.min(remaining, batch.getQuantityInStock());
            deductStock(batch.getInventoryId(), deduct);
            deductions.add(new StockDeduction(batch, deduct));
            remaining -= deduct;
        }

        if (remaining > 0) {
            Drug drug = drugRepository.findById(drugId).orElse(null);
            String name = drug != null ? drug.getDrugName() : String.valueOf(drugId);
            throw new InsufficientStockException("Insufficient stock for: " + name);
        }
        return deductions;
    }

    public List<StockAlertDTO> getLowStockAlerts() {
        return inventoryRepository.findLowStockItems().stream()
            .map(i -> StockAlertDTO.builder()
                .drugName(i.getDrug().getDrugName())
                .batchNumber(i.getBatchNumber())
                .currentStock(i.getQuantityInStock())
                .reorderLevel(i.getReorderLevel())
                .status(i.getStockStatus().name())
                .expiryDate(i.getExpiryDate())
                .build())
            .collect(Collectors.toList());
    }

    public List<StockAlertDTO> getExpiringAlerts(int withinDays) {
        LocalDate threshold = LocalDate.now().plusDays(withinDays);
        return inventoryRepository.findExpiringBatches(LocalDate.now(), threshold).stream()
            .map(i -> StockAlertDTO.builder()
                .drugName(i.getDrug().getDrugName())
                .batchNumber(i.getBatchNumber())
                .currentStock(i.getQuantityInStock())
                .reorderLevel(i.getReorderLevel())
                .status("EXPIRING_SOON")
                .expiryDate(i.getExpiryDate())
                .build())
            .collect(Collectors.toList());
    }

    public Page<InventoryDTO> getInventoryByStatus(StockStatus status, Pageable pageable) {
        return inventoryRepository.findByStockStatus(status, pageable).map(this::mapToDTO);
    }

    public Page<InventoryDTO> getAllInventory(Pageable pageable) {
        return inventoryRepository.findAll(pageable).map(this::mapToDTO);
    }

    public List<InventoryDTO> getInventoryForDrug(Long drugId) {
        return inventoryRepository.findByDrug_DrugIdOrderByExpiryDateAsc(drugId)
            .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // ---- Scheduled Jobs ----
    @Scheduled(cron = "0 0 6 * * *")   // Every day at 6 AM
    @Transactional
    public void markExpiredBatches() {
        List<Inventory> expired = inventoryRepository.findExpiredBatches(LocalDate.now());
        expired.forEach(i -> i.setStockStatus(StockStatus.EXPIRED));
        inventoryRepository.saveAll(expired);
        log.info("Marked {} batches as EXPIRED", expired.size());
    }

    // ---- Mapping ----
    public InventoryDTO mapToDTO(Inventory i) {
        InventoryDTO dto = new InventoryDTO();
        dto.setInventoryId(i.getInventoryId());
        dto.setDrugId(i.getDrug().getDrugId());
        dto.setDrugName(i.getDrug().getDrugName());
        if (i.getSupplier() != null) {
            dto.setSupplierId(i.getSupplier().getSupplierId());
            dto.setSupplierName(i.getSupplier().getSupplierName());
        }
        dto.setBatchNumber(i.getBatchNumber());
        dto.setQuantityInStock(i.getQuantityInStock());
        dto.setReorderLevel(i.getReorderLevel());
        dto.setExpiryDate(i.getExpiryDate());
        dto.setManufacturingDate(i.getManufacturingDate());
        dto.setPurchasePrice(i.getPurchasePrice());
        dto.setSellingPrice(i.getSellingPrice());
        dto.setStorageLocation(i.getStorageLocation());
        dto.setStockStatus(i.getStockStatus().name());
        return dto;
    }
}


