package com.medicare.controller;

import com.medicare.service.impl.DatabaseConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/backup/db")
@RequiredArgsConstructor
@Tag(name = "Database Config", description = "Switch active database connection")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class DatabaseConfigController {

    private final DatabaseConfigService dbConfigService;

    @GetMapping("/status")
    @Operation(summary = "Get current active database profile and connection info")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(dbConfigService.getCurrentStatus());
    }

    @PostMapping("/test")
    @Operation(summary = "Test a database connection without switching")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, String> config) {
        return ResponseEntity.ok(dbConfigService.testConnection(config));
    }

    @PostMapping("/switch")
    @Operation(summary = "Save new database config and schedule application restart")
    public ResponseEntity<Map<String, Object>> switchDatabase(@RequestBody Map<String, String> config) {
        return ResponseEntity.ok(dbConfigService.saveAndRestart(config));
    }
}
