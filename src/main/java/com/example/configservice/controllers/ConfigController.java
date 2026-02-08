package com.example.configservice.controllers;

import com.example.configservice.dto.ConfigSource;
import com.example.configservice.services.ConfigRepoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/config")
public class ConfigController {

    private final ConfigRepoService service;

    public ConfigController(ConfigRepoService service) {
        this.service = service;
    }

    /**
     * GET /loadConfig/{app}/{profile}
     * <p>
     * Loads merged configuration for an application and profile.
     * <p>
     * Parameters:
     * format=properties -> merged properties text (default)
     * format=yaml       -> merged YAML
     * format=json       -> merged JSON
     * resolvePlaceholders=true -> resolve ${ENV_VAR} and ${app.property} placeholders
     */
    @GetMapping("/get/{app}/{profile}")
    public ResponseEntity<?> get(
            @PathVariable String app,
            @PathVariable String profile,
            @RequestParam(defaultValue = "yaml") String format
    ) {
        try {
            switch (format.toLowerCase(Locale.ROOT)) {
                case "properties" -> {
                    var flat = service.mergedFlat(app, profile);
                    return ResponseEntity.ok()
                            .contentType(MediaType.TEXT_PLAIN)
                            .body(service.toPropertiesText(flat));
                }
                case "json" -> {
                    var flat = service.mergedFlat(app, profile);
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("application", app);
                    response.put("profile", profile);
                    response.put("properties", flat);
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(service.toJson(response));
                }
                case "yaml" -> {
                    var deep = service.mergedDeep(app, profile);
                    return ResponseEntity.ok()
                            .contentType(MediaType.valueOf("text/yaml"))
                            .body(service.toYaml(deep));
                }
                default -> {
                    return ResponseEntity.badRequest()
                            .contentType(MediaType.TEXT_PLAIN)
                            .body("Unsupported format. Use properties|json|yaml");
                }
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * GET /loadConfig/sources/{app}/{profile}
     * <p>
     * Loads non-merged configuration sources (each file separately).
     * Shows which configuration file each property comes from.
     * Always returns JSON format.
     */
    @GetMapping("/get/sources/{app}/{profile}")
    public ResponseEntity<?> getSources(
            @PathVariable String app,
            @PathVariable String profile
    ) {
        try {
            List<ConfigSource> sources = service.loadSources(app, profile);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("app", app);
            body.put("profile", profile);
            body.put("propertySources", sources);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

}
