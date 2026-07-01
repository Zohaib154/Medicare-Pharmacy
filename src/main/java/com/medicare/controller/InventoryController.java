package com.medicare.controller;

import com.medicare.dto.InventoryDTO;
import com.medicare.dto.StockAlertDTO;
import com.medicare.entity.Inventory.StockStatus;
import com.medicare.service.impl.InventoryServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Stock management and batch tracking")
@SecurityRequirement(name = "BearerAuth")
public class InventoryController {

    private final InventoryServiceImpl inventoryService;

    @PostMapping
    @Operation(summary = "Add a new stock batch")
    public ResponseEntity<InventoryDTO> addStock(@Valid @RequestBody InventoryDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.addStock(dto));
    }

    @GetMapping
    @Operation(summary = "Get all inventory batches")
    public ResponseEntity<Page<InventoryDTO>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "100") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("expiryDate").ascending());
        return ResponseEntity.ok(inventoryService.getAllInventory(pageable));
    }

    @GetMapping("/drug/{drugId}")
    @Operation(summary = "Get all batches for a specific drug")
    public ResponseEntity<List<InventoryDTO>> getByDrug(@PathVariable Long drugId) {
        return ResponseEntity.ok(inventoryService.getInventoryForDrug(drugId));
    }

    @GetMapping("/alerts/low-stock")
    @Operation(summary = "Get all low-stock and out-of-stock drugs")
    public ResponseEntity<List<StockAlertDTO>> getLowStock() {
        return ResponseEntity.ok(inventoryService.getLowStockAlerts());
    }

    @GetMapping("/alerts/expiring")
    @Operation(summary = "Get drugs expiring within N days (default 30)")
    public ResponseEntity<List<StockAlertDTO>> getExpiring(
        @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(inventoryService.getExpiringAlerts(days));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get inventory by stock status")
    public ResponseEntity<Page<InventoryDTO>> getByStatus(
        @PathVariable String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(inventoryService.getInventoryByStatus(
            StockStatus.valueOf(status.toUpperCase()), pageable));
    }
}


