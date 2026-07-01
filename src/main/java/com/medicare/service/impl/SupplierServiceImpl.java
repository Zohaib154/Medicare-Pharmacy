package com.medicare.service.impl;

import com.medicare.dto.PurchaseOrderDTO;
import com.medicare.dto.PurchaseOrderItemDTO;
import com.medicare.dto.SupplierDTO;
import com.medicare.entity.*;
import com.medicare.entity.PurchaseOrder.OrderStatus;
import com.medicare.exception.BusinessException;
import com.medicare.exception.DuplicateResourceException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SupplierServiceImpl {

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final DrugRepository drugRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;

    private static final AtomicLong PO_COUNTER = new AtomicLong(2000);

    /** Sync counter with DB on startup so PO numbers never collide after a restart */
    @PostConstruct
    void initPoCounter() {
        try {
            long max = purchaseOrderRepository.findAllPoNumbers().stream()
                .filter(n -> n != null && n.startsWith("PO-"))
                .mapToLong(n -> {
                    try { return Long.parseLong(n.substring(3)); }
                    catch (NumberFormatException e) { return 0L; }
                })
                .max()
                .orElse(2000L);
            PO_COUNTER.set(max);
            log.info("PO counter initialized to {}", max);
        } catch (Exception e) {
            log.warn("Could not initialize PO counter from DB, using default 2000: {}", e.getMessage());
            PO_COUNTER.set(2000L);
        }
    }

    // ======================== SUPPLIER ========================

    @Transactional
    public SupplierDTO createSupplier(SupplierDTO dto) {
        if (supplierRepository.existsBySupplierNameIgnoreCase(dto.getSupplierName())) {
            throw new DuplicateResourceException("Supplier already exists: " + dto.getSupplierName());
        }
        Supplier supplier = mapSupplierToEntity(dto);
        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier created: {}", saved.getSupplierName());
        return mapSupplierToDTO(saved);
    }

    @Transactional
    public SupplierDTO updateSupplier(Long id, SupplierDTO dto) {
        Supplier s = supplierRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));
        s.setSupplierName(dto.getSupplierName());
        s.setContactPerson(dto.getContactPerson());
        s.setContactNumber(dto.getContactNumber());
        s.setEmail(dto.getEmail());
        s.setAddress(dto.getAddress());
        s.setCity(dto.getCity());
        s.setLicenseNumber(dto.getLicenseNumber());
        s.setGstNumber(dto.getGstNumber());
        s.setPaymentTerms(dto.getPaymentTerms());
        s.setNotes(dto.getNotes());
        if (dto.getIsActive() != null) {
            s.setIsActive(dto.getIsActive());
        }
        return mapSupplierToDTO(supplierRepository.save(s));
    }

    public SupplierDTO getSupplierById(Long id) {
        return supplierRepository.findById(id)
            .map(this::mapSupplierToDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));
    }

    public Page<SupplierDTO> getAllSuppliers(Pageable pageable) {
        return supplierRepository.findByIsActiveTrue(pageable).map(this::mapSupplierToDTO);
    }

    public Page<SupplierDTO> getAllSuppliersIncludingInactive(Pageable pageable) {
        return supplierRepository.findAll(pageable).map(this::mapSupplierToDTO);
    }

    public Page<SupplierDTO> searchSuppliers(String query, Pageable pageable) {
        return supplierRepository.searchSuppliers(query, pageable).map(this::mapSupplierToDTO);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        Supplier s = supplierRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));
        s.setIsActive(false);
        supplierRepository.save(s);
    }

    // ======================== PURCHASE ORDERS ========================

    @Transactional
    public PurchaseOrderDTO createPurchaseOrder(PurchaseOrderDTO dto) {
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + dto.getSupplierId()));
        User orderedBy = userRepository.findById(dto.getOrderedById())
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + dto.getOrderedById()));

        String poNumber = "PO-" + PO_COUNTER.incrementAndGet();

        PurchaseOrder po = PurchaseOrder.builder()
            .poNumber(poNumber)
            .supplier(supplier)
            .orderedBy(orderedBy)
            .orderDate(LocalDate.now())
            .expectedDeliveryDate(dto.getExpectedDeliveryDate())
            .notes(dto.getNotes())
            .items(new ArrayList<>())
            .build();

        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItemDTO itemDTO : dto.getItems()) {
            Drug drug = drugRepository.findById(itemDTO.getDrugId())
                .orElseThrow(() -> new ResourceNotFoundException("Drug not found: " + itemDTO.getDrugId()));
            BigDecimal lineTotal = itemDTO.getUnitPrice()
                .multiply(BigDecimal.valueOf(itemDTO.getOrderedQuantity()));

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                .purchaseOrder(po)
                .drug(drug)
                .orderedQuantity(itemDTO.getOrderedQuantity())
                .unitPrice(itemDTO.getUnitPrice())
                .totalPrice(lineTotal)
                .build();

            po.getItems().add(item);
            total = total.add(lineTotal);
        }

        po.setTotalAmount(total);
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        log.info("Purchase Order created: {} for supplier: {}", poNumber, supplier.getSupplierName());
        return mapPOToDTO(saved);
    }

    /**
     * Simple status-only update — used by the frontend dropdown.
     * No inventory side effects. If the new status is DELIVERED, also
     * sets the actualDeliveryDate so the record stays consistent.
     */
    @Transactional
    public PurchaseOrderDTO updateOrderStatus(Long poId, String newStatus) {
        PurchaseOrder po = purchaseOrderRepository.findByIdWithDetails(poId)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found: " + poId));

        OrderStatus parsedStatus;
        try {
            parsedStatus = OrderStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid status value: " + newStatus +
                ". Valid values: PENDING, CONFIRMED, SHIPPED, DELIVERED, PARTIALLY_DELIVERED, CANCELLED");
        }

        po.setStatus(parsedStatus);
        if (parsedStatus == OrderStatus.DELIVERED && po.getActualDeliveryDate() == null) {
            po.setActualDeliveryDate(LocalDate.now());
        }
        purchaseOrderRepository.saveAndFlush(po);
        log.info("Purchase Order {} status changed to {}", po.getPoNumber(), parsedStatus);

        return purchaseOrderRepository.findByIdWithDetails(poId)
            .map(this::mapPOToDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found after save: " + po.getPoNumber()));
    }

    @Transactional
    public PurchaseOrderDTO receiveOrder(Long poId) {
        // Load PO with all details
        PurchaseOrder po = purchaseOrderRepository.findByIdWithDetails(poId)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found: " + poId));

        if (po.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("Order already received: " + po.getPoNumber());
        }
        if (po.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Cannot receive a cancelled order: " + po.getPoNumber());
        }
        if (po.getItems() == null || po.getItems().isEmpty()) {
            throw new BusinessException("Purchase order has no items to receive: " + po.getPoNumber());
        }

        // Mark status DELIVERED and set delivery date
        po.setStatus(OrderStatus.DELIVERED);
        po.setActualDeliveryDate(LocalDate.now());
        purchaseOrderRepository.saveAndFlush(po);
        log.info("Purchase Order status set to DELIVERED: {}", po.getPoNumber());

        // Create inventory batch entries for each ordered item
        for (PurchaseOrderItem item : po.getItems()) {
            String batchSuffix = item.getPoItemId() != null
                ? item.getPoItemId().toString()
                : item.getDrug().getDrugId().toString();
            String batchNumber = "BATCH-" + po.getPoNumber() + "-" + batchSuffix;

            // Skip if a batch with this number already exists (idempotent re-receive guard)
            if (inventoryRepository.findByBatchNumber(batchNumber).isPresent()) {
                log.warn("Inventory batch {} already exists, skipping duplicate creation.", batchNumber);
            } else {
                Inventory inventory = Inventory.builder()
                    .drug(item.getDrug())
                    .supplier(po.getSupplier())
                    .batchNumber(batchNumber)
                    .quantityInStock(item.getOrderedQuantity())
                    .reorderLevel(50)
                    .expiryDate(LocalDate.now().plusYears(2))
                    .manufacturingDate(LocalDate.now())
                    .purchasePrice(item.getUnitPrice())
                    .sellingPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(1.3)))
                    .build();
                inventoryRepository.save(inventory);
                log.info("Inventory batch {} created for drug: {}", batchNumber, item.getDrug().getDrugName());
            }
            item.setReceivedQuantity(item.getOrderedQuantity());
        }

        // Re-fetch to return the fully updated DTO
        return purchaseOrderRepository.findByIdWithDetails(poId)
            .map(this::mapPOToDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found after save: " + po.getPoNumber()));
    }

    public Page<PurchaseOrderDTO> getAllPurchaseOrders(Pageable pageable) {
        // Use a fetch-join query to avoid LazyInitializationException on supplier/orderedBy
        List<PurchaseOrder> all = purchaseOrderRepository.findAllWithDetails();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<PurchaseOrderDTO> dtos = all.subList(start, end).stream()
            .map(this::mapPOToDTO)
            .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, all.size());
    }

    public PurchaseOrderDTO getPurchaseOrderById(Long id) {
        return purchaseOrderRepository.findByIdWithDetails(id)
            .map(this::mapPOToDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found: " + id));
    }

    // ---- Mapping ----
    private Supplier mapSupplierToEntity(SupplierDTO dto) {
        return Supplier.builder()
            .supplierName(dto.getSupplierName())
            .contactPerson(dto.getContactPerson())
            .contactNumber(dto.getContactNumber())
            .email(dto.getEmail())
            .address(dto.getAddress())
            .city(dto.getCity())
            .licenseNumber(dto.getLicenseNumber())
            .gstNumber(dto.getGstNumber())
            .paymentTerms(dto.getPaymentTerms())
            .notes(dto.getNotes())
            .build();
    }

    public SupplierDTO mapSupplierToDTO(Supplier s) {
        SupplierDTO dto = new SupplierDTO();
        dto.setSupplierId(s.getSupplierId());
        dto.setSupplierName(s.getSupplierName());
        dto.setContactPerson(s.getContactPerson());
        dto.setContactNumber(s.getContactNumber());
        dto.setEmail(s.getEmail());
        dto.setAddress(s.getAddress());
        dto.setCity(s.getCity());
        dto.setLicenseNumber(s.getLicenseNumber());
        dto.setGstNumber(s.getGstNumber());
        dto.setPaymentTerms(s.getPaymentTerms());
        dto.setOutstandingBalance(s.getOutstandingBalance());
        dto.setIsActive(s.getIsActive());
        dto.setNotes(s.getNotes());
        return dto;
    }

    private PurchaseOrderDTO mapPOToDTO(PurchaseOrder po) {
        PurchaseOrderDTO dto = new PurchaseOrderDTO();
        dto.setOrderId(po.getOrderId());
        dto.setPoNumber(po.getPoNumber());
        dto.setSupplierId(po.getSupplier().getSupplierId());
        dto.setSupplierName(po.getSupplier().getSupplierName());
        dto.setOrderedById(po.getOrderedBy().getUserId());
        dto.setOrderedByName(po.getOrderedBy().getFullName());
        dto.setOrderDate(po.getOrderDate());
        dto.setExpectedDeliveryDate(po.getExpectedDeliveryDate());
        dto.setActualDeliveryDate(po.getActualDeliveryDate());
        dto.setStatus(po.getStatus().name());
        dto.setTotalAmount(po.getTotalAmount());
        dto.setPaidAmount(po.getPaidAmount());
        dto.setNotes(po.getNotes());
        if (po.getItems() != null) {
            dto.setItems(po.getItems().stream().map(item -> {
                PurchaseOrderItemDTO i = new PurchaseOrderItemDTO();
                i.setPoItemId(item.getPoItemId());
                i.setDrugId(item.getDrug().getDrugId());
                i.setDrugName(item.getDrug().getDrugName());
                i.setOrderedQuantity(item.getOrderedQuantity());
                i.setReceivedQuantity(item.getReceivedQuantity());
                i.setUnitPrice(item.getUnitPrice());
                i.setTotalPrice(item.getTotalPrice());
                return i;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}


