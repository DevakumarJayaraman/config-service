package com.example.configservice.controllers;

import com.example.configservice.services.ComparisonService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for configuration comparison APIs.
 * Provides endpoints to compare configurations across profiles and applications.
 */
@RestController
@RequestMapping("/config/compare")
public class ComparisonController {

    private final ComparisonService comparisonService;

    public ComparisonController(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    /**
     * Compare raw sources (file-to-file) between two profiles for a specific application.
     * <p>
     * GET /config/compare/source/{app}/{profileA}/{profileB}
     * <p>
     * Example: http://127.0.0.1:8888/config/compare/source/order-service/uat/prod
     * <p>
     * Shows what's in order-service_uat vs order-service_prod files (no merging)
     *
     * @param app      the application name (e.g., "order-service")
     * @param profileA the first profile (e.g., "uat")
     * @param profileB the second profile (e.g., "prod")
     * @return comparison result with differences
     */
    @GetMapping("/sources/{app}/{profileA}/{profileB}")
    public ResponseEntity<?> compareSourcesForApp(
            @PathVariable String app,
            @PathVariable String profileA,
            @PathVariable String profileB
    ) {
        try {
            Map<String, Object> result;
            if (app != null && app.equalsIgnoreCase("all")) {
                result = comparisonService.compareSourcesForAllApps(profileA, profileB);
            } else {
                result = comparisonService.compareSourcesForApp(app, profileA, profileB);
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error comparing sources: " + e.getMessage()));
        }
    }
    
    /**
     * Compare merged values between two profiles for a specific application.
     * Shows merged configuration with source tracking for each property.
     * <p>
     * GET /config/compare/values/{app}/{profileA}/{profileB}
     * <p>
     * Example: http://127.0.0.1:8888/config/compare/values/order-service/uat/prod
     * <p>
     * Shows merged order-service config for uat vs prod (with source tracking)
     *
     * @param app      the application name (e.g., "order-service")
     * @param profileA the first profile (e.g., "uat")
     * @param profileB the second profile (e.g., "prod")
     * @return merged config comparison with source details
     */
    @GetMapping("/values/{app}/{profileA}/{profileB}")
    public ResponseEntity<?> compareValuesForApp(
            @PathVariable String app,
            @PathVariable String profileA,
            @PathVariable String profileB
    ) {
        try {
            Map<String, Object> result;
            if (app != null && app.equalsIgnoreCase("all")) {
                result = comparisonService.compareValuesForAllApps(profileA, profileB);
            } else {
                result = comparisonService.compareValuesForApp(app, profileA, profileB);
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error comparing values: " + e.getMessage()));
        }
    }
}