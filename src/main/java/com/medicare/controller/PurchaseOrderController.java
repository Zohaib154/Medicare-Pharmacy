package com.medicare.controller;

import com.medicare.dto.PurchaseOrderDTO;
import com.medicare.service.impl.SupplierServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Purchase Orders", description = "Manage stock purchase orders from suppliers")
@SecurityRequirement(name = "BearerAuth")
public class PurchaseOrderController {

    private final SupplierServiceImpl supplierService;

    @PostMapping
    @Operation(summary = "Create a new purchase order")
    public ResponseEntity<PurchaseOrderDTO> create(@Valid @RequestBody PurchaseOrderDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.createPurchaseOrder(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase order by ID")
    public ResponseEntity<PurchaseOrderDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getPurchaseOrderById(id));
    }

    @GetMapping
    @Operation(summary = "List all purchase orders")
    public ResponseEntity<Page<PurchaseOrderDTO>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(supplierService.getAllPurchaseOrders(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderDate"))));
    }

    /**
     * Simple status-only update — no inventory side effects.
     * Used by the frontend status dropdown.
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Update the delivery status of a purchase order")
    public ResponseEntity<PurchaseOrderDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(supplierService.updateOrderStatus(id, status));
    }

    /**
     * Full receive: sets status to DELIVERED and auto-creates inventory batches.
     */
    @PutMapping("/{id}/receive")
    @Operation(summary = "Receive an order and automatically create inventory batches")
    public ResponseEntity<PurchaseOrderDTO> receive(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.receiveOrder(id));
    }
}
