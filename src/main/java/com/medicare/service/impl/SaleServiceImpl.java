package com.medicare.service.impl;

import com.medicare.dto.SaleDTO;
import com.medicare.dto.SaleItemDTO;
import com.medicare.entity.*;
import com.medicare.entity.Sale.SaleStatus;
import com.medicare.exception.BusinessException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.*;
import com.medicare.service.DocumentNumberService;
import com.medicare.service.StockDeduction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SaleServiceImpl {

    private final SaleRepository saleRepository;
    private final PatientRepository patientRepository;
    private final DrugRepository drugRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final InventoryServiceImpl inventoryService;
    private final DocumentNumberService documentNumberService;

    @Transactional
    public SaleDTO createSale(SaleDTO dto) {
        User soldBy = userRepository.findById(dto.getSoldById())
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + dto.getSoldById()));

        Sale sale = Sale.builder()
            .billNumber(documentNumberService.nextBillNumber())
            .soldBy(soldBy)
            .saleDateTime(LocalDateTime.now())
            .paymentMethod(dto.getPaymentMethod() != null
                ? Sale.PaymentMethod.valueOf(dto.getPaymentMethod())
                : Sale.PaymentMethod.CASH)
            .notes(dto.getNotes())
            .items(new ArrayList<>())
            .build();

        // Optional: link patient
        if (dto.getPatientId() != null) {
            patientRepository.findById(dto.getPatientId()).ifPresent(sale::setPatient);
        }

        // Optional: link prescription
        if (dto.getPrescriptionId() != null) {
            prescriptionRepository.findById(dto.getPrescriptionId()).ifPresent(sale::setPrescription);
        }

        // Build sale items and deduct stock
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;

        for (SaleItemDTO itemDTO : dto.getItems()) {
            Drug drug = drugRepository.findById(itemDTO.getDrugId())
                .orElseThrow(() -> new ResourceNotFoundException("Drug not found: " + itemDTO.getDrugId()));

            List<StockDeduction> deductions = inventoryService.deductStockFefo(drug.getDrugId(), itemDTO.getQuantity());

            BigDecimal unitPrice = itemDTO.getUnitPrice() != null ? itemDTO.getUnitPrice() : drug.getMrp();
            BigDecimal discPct = itemDTO.getDiscountPercent() != null ? itemDTO.getDiscountPercent() : BigDecimal.ZERO;

            for (StockDeduction deduction : deductions) {
                int qty = deduction.getQuantity();
                BigDecimal lineTotal = unitPrice
                    .multiply(BigDecimal.valueOf(qty))
                    .multiply(BigDecimal.ONE.subtract(discPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));

                BigDecimal gstAmt = BigDecimal.ZERO;
                if (drug.getGstPercent() != null) {
                    gstAmt = lineTotal.multiply(drug.getGstPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }

                SaleItem saleItem = SaleItem.builder()
                    .sale(sale)
                    .drug(drug)
                    .inventory(deduction.getBatch())
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .discountPercent(discPct)
                    .gstAmount(gstAmt)
                    .totalPrice(lineTotal.add(gstAmt))
                    .build();

                sale.getItems().add(saleItem);
                subtotal = subtotal.add(lineTotal);
                totalGst = totalGst.add(gstAmt);
            }
        }

        // Apply bill-level discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal discountPct = dto.getDiscountPercent() != null ? dto.getDiscountPercent() : BigDecimal.ZERO;
        if (discountPct.compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = subtotal.multiply(discountPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal total = subtotal.subtract(discountAmount).add(totalGst);

        sale.setSubtotal(subtotal);
        sale.setDiscountPercent(discountPct);
        sale.setDiscountAmount(discountAmount);
        sale.setGstAmount(totalGst);
        sale.setTotalAmount(total);
        sale.setAmountPaid(dto.getAmountPaid() != null ? dto.getAmountPaid() : total);
        sale.setChangeReturned(sale.getAmountPaid().subtract(total).max(BigDecimal.ZERO));
        sale.setStatus(SaleStatus.COMPLETED);

        Sale saved = saleRepository.save(sale);
        log.info("Sale created: {} | Total: {} | Payment: {}", saved.getBillNumber(), total, sale.getPaymentMethod());
        return mapToDTO(saved);
    }

    public SaleDTO getSaleById(Long id) {
        return saleRepository.findById(id)
            .map(this::mapToDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + id));
    }

    public SaleDTO getSaleByBillNumber(String billNumber) {
        return saleRepository.findByBillNumber(billNumber)
            .map(this::mapToDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billNumber));
    }

    public Page<SaleDTO> getAllSales(Pageable pageable) {
        return saleRepository.findAll(pageable).map(this::mapToDTO);
    }

    public Page<SaleDTO> getSalesByPatient(Long patientId, Pageable pageable) {
        return saleRepository.findByPatient_PatientId(patientId, pageable).map(this::mapToDTO);
    }

    public List<SaleDTO> getSalesBetween(LocalDateTime start, LocalDateTime end) {
        return saleRepository.findSalesBetween(start, end)
            .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public SaleDTO refundSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + saleId));
        if (sale.getStatus() == SaleStatus.REFUNDED) {
            throw new BusinessException("Sale already refunded: " + sale.getBillNumber());
        }
        sale.setStatus(SaleStatus.REFUNDED);
        // Re-stock items
        for (SaleItem item : sale.getItems()) {
            if (item.getInventory() != null) {
                Inventory inv = item.getInventory();
                inv.setQuantityInStock(inv.getQuantityInStock() + item.getQuantity());
                inventoryRepository.save(inv);
            }
        }
        log.info("Sale refunded: {}", sale.getBillNumber());
        return mapToDTO(saleRepository.save(sale));
    }

    // ---- Mapping ----
    public SaleDTO mapToDTO(Sale s) {
        SaleDTO dto = new SaleDTO();
        dto.setSaleId(s.getSaleId());
        dto.setBillNumber(s.getBillNumber());
        dto.setSaleDateTime(s.getSaleDateTime());
        dto.setPaymentMethod(s.getPaymentMethod().name());
        dto.setStatus(s.getStatus().name());
        dto.setSubtotal(s.getSubtotal());
        dto.setDiscountPercent(s.getDiscountPercent());
        dto.setDiscountAmount(s.getDiscountAmount());
        dto.setGstAmount(s.getGstAmount());
        dto.setTotalAmount(s.getTotalAmount());
        dto.setAmountPaid(s.getAmountPaid());
        dto.setChangeReturned(s.getChangeReturned());
        dto.setNotes(s.getNotes());
        dto.setSoldByName(s.getSoldBy().getFullName());
        if (s.getPatient() != null) {
            dto.setPatientId(s.getPatient().getPatientId());
            dto.setPatientName(s.getPatient().getFullName());
        }
        if (s.getPrescription() != null) {
            dto.setPrescriptionId(s.getPrescription().getPrescriptionId());
            dto.setRxNumber(s.getPrescription().getRxNumber());
        }
        if (s.getItems() != null) {
            dto.setItems(s.getItems().stream().map(item -> {
                SaleItemDTO i = new SaleItemDTO();
                i.setSaleItemId(item.getSaleItemId());
                i.setDrugId(item.getDrug().getDrugId());
                i.setDrugName(item.getDrug().getDrugName());
                i.setQuantity(item.getQuantity());
                i.setUnitPrice(item.getUnitPrice());
                i.setDiscountPercent(item.getDiscountPercent());
                i.setGstAmount(item.getGstAmount());
                i.setTotalPrice(item.getTotalPrice());
                return i;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}


