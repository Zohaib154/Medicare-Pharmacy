package com.medicare.controller;

import com.medicare.dto.SaleDTO;
import com.medicare.service.impl.SaleServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
@Tag(name = "Sales & Billing", description = "Point-of-sale and billing operations")
@SecurityRequirement(name = "BearerAuth")
public class SaleController {

    private final SaleServiceImpl saleService;

    @PostMapping
    @Operation(summary = "Create a new sale / bill")
    public ResponseEntity<SaleDTO> create(@Valid @RequestBody SaleDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.createSale(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get sale by ID")
    public ResponseEntity<SaleDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getSaleById(id));
    }

    @GetMapping("/bill/{billNumber}")
    @Operation(summary = "Get sale by bill number")
    public ResponseEntity<SaleDTO> getByBillNumber(@PathVariable String billNumber) {
        return ResponseEntity.ok(saleService.getSaleByBillNumber(billNumber));
    }

    @GetMapping
    @Operation(summary = "List all sales (paginated)")
    public ResponseEntity<Page<SaleDTO>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(saleService.getAllSales(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "saleDateTime"))));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get sales for a specific patient")
    public ResponseEntity<Page<SaleDTO>> getByPatient(
        @PathVariable Long patientId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(saleService.getSalesByPatient(patientId, PageRequest.of(page, size)));
    }

    @GetMapping("/range")
    @Operation(summary = "Get sales in a date-time range")
    public ResponseEntity<List<SaleDTO>> getByRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ResponseEntity.ok(saleService.getSalesBetween(start, end));
    }

    @PutMapping("/{id}/refund")
    @Operation(summary = "Process a refund and restock items")
    public ResponseEntity<SaleDTO> refund(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.refundSale(id));
    }
}


