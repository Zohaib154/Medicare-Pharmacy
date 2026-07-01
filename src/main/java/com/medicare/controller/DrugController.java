package com.medicare.controller;

import com.medicare.dto.DrugDTO;
import com.medicare.service.DrugService;
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
@RequestMapping("/drugs")
@RequiredArgsConstructor
@Tag(name = "Drug Catalogue", description = "Manage the drug catalogue")
@SecurityRequirement(name = "BearerAuth")
public class DrugController {

    private final DrugService drugService;

    @PostMapping
    @Operation(summary = "Add a new drug")
    public ResponseEntity<DrugDTO> create(@Valid @RequestBody DrugDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(drugService.createDrug(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get drug by ID")
    public ResponseEntity<DrugDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(drugService.getDrugById(id));
    }

    @GetMapping("/with-stock")
    @Operation(summary = "List active drugs with total stock from inventory")
    public ResponseEntity<List<DrugDTO>> getWithStock() {
        return ResponseEntity.ok(drugService.getDrugsWithStock());
    }

    @GetMapping("/all")
    @Operation(summary = "List all drugs including inactive (admin/manager only)")
    public ResponseEntity<Page<DrugDTO>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "200") int size,
        @RequestParam(defaultValue = "drugName") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return ResponseEntity.ok(drugService.getAllDrugsIncludingInactive(pageable));
    }

    @GetMapping
    @Operation(summary = "List active drugs (paginated)")
    public ResponseEntity<Page<DrugDTO>> getActive(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "drugName") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return ResponseEntity.ok(drugService.getAllDrugs(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search drugs by name, category, or manufacturer")
    public ResponseEntity<Page<DrugDTO>> search(
        @RequestParam String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(drugService.searchDrugs(q, pageable));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Filter drugs by category")
    public ResponseEntity<Page<DrugDTO>> byCategory(
        @PathVariable String category,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(drugService.getDrugsByCategory(category, pageable));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get all drug categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(drugService.getAllCategories());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update drug details")
    public ResponseEntity<DrugDTO> update(@PathVariable Long id, @Valid @RequestBody DrugDTO dto) {
        return ResponseEntity.ok(drugService.updateDrug(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate (soft-delete) a drug")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        drugService.deleteDrug(id);
        return ResponseEntity.noContent().build();
    }

}


