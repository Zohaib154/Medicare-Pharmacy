package com.medicare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persists application settings (pharmacy name, invoice footer, etc.)
 * to ~/.medicare/app-settings.json so they survive application restarts
 * regardless of which port the embedded server uses.
 */
@RestController
@RequestMapping("/settings")
@Tag(name = "App Settings", description = "Persistent application settings")
@SecurityRequirement(name = "BearerAuth")
@Slf4j
public class AppSettingsController {

    private static final File SETTINGS_FILE =
        new File(System.getProperty("user.home") + "/.medicare/app-settings.json");

    private final ObjectMapper mapper = new ObjectMapper();

    /** Default values returned when no settings file exists yet */
    private Map<String, Object> defaults() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("hospitalName",   "MediCare Pharmacy");
        d.put("ownerName",      "");
        d.put("address",        "");
        d.put("phone",          "");
        d.put("email",          "");
        d.put("invoiceFooter",  "Thank you for choosing us. Get well soon!");
        d.put("currencySymbol", "$");
        d.put("taxLabel",       "GST");
        d.put("licenseNumber",  "");
        return d;
    }

    @GetMapping
    @Operation(summary = "Load persisted application settings")
    public ResponseEntity<Map<String, Object>> load() {
        try {
            if (SETTINGS_FILE.exists()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> saved = mapper.readValue(SETTINGS_FILE, Map.class);
                // Merge with defaults so new keys are always present
                Map<String, Object> merged = defaults();
                merged.putAll(saved);
                return ResponseEntity.ok(merged);
            }
        } catch (Exception e) {
            log.warn("Could not read settings file, returning defaults: {}", e.getMessage());
        }
        return ResponseEntity.ok(defaults());
    }

    @PostMapping
    @Operation(summary = "Save application settings")
    public ResponseEntity<Map<String, Object>> save(@RequestBody Map<String, Object> settings) {
        try {
            // Ensure parent directory exists
            SETTINGS_FILE.getParentFile().mkdirs();
            // Merge over defaults so we never lose known keys
            Map<String, Object> toSave = defaults();
            toSave.putAll(settings);
            mapper.writerWithDefaultPrettyPrinter().writeValue(SETTINGS_FILE, toSave);
            log.info("App settings saved to {}", SETTINGS_FILE.getAbsolutePath());
            return ResponseEntity.ok(toSave);
        } catch (Exception e) {
            log.error("Failed to save settings: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
