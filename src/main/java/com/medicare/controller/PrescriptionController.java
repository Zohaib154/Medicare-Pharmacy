package com.medicare.controller;

import com.medicare.dto.PrescriptionDTO;
import com.medicare.entity.Prescription.PrescriptionStatus;
import com.medicare.service.impl.PrescriptionServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prescriptions")
@RequiredArgsConstructor
@Tag(name = "Prescriptions", description = "Prescription creation and dispensing")
@SecurityRequirement(name = "BearerAuth")
public class PrescriptionController {

    private final PrescriptionServiceImpl prescriptionService;

    @PostMapping
    @Operation(summary = "Create a new prescription")
    public ResponseEntity<PrescriptionDTO> create(@Valid @RequestBody PrescriptionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prescriptionService.createPrescription(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get prescription by ID")
    public ResponseEntity<PrescriptionDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionById(id));
    }

    @GetMapping
    @Operation(summary = "List all prescriptions (paginated)")
    public ResponseEntity<Page<PrescriptionDTO>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(prescriptionService.getAllPrescriptions(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filter prescriptions by status")
    public ResponseEntity<Page<PrescriptionDTO>> getByStatus(
        @PathVariable String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionsByStatus(
            PrescriptionStatus.valueOf(status.toUpperCase()),
            PageRequest.of(page, size)));
    }

    @GetMapping("/today")
    @Operation(summary = "Get all prescriptions created today")
    public ResponseEntity<List<PrescriptionDTO>> getToday() {
        return ResponseEntity.ok(prescriptionService.getTodaysPrescriptions());
    }

    @PutMapping("/{id}/dispense")
    @Operation(summary = "Dispense a prescription (deducts stock)")
    public ResponseEntity<PrescriptionDTO> dispense(
        @PathVariable Long id,
        @RequestParam Long pharmacistId
    ) {
        return ResponseEntity.ok(prescriptionService.dispensePrescription(id, pharmacistId));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pending prescription")
    public ResponseEntity<PrescriptionDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(prescriptionService.cancelPrescription(id));
    }
}


