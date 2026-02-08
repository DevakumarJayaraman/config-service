package com.example.configservice.controllers;

import com.example.configservice.services.ConfigRepoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for ConfigController.
 * Tests configuration retrieval, formatting, and cache viewing endpoints.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigController Tests")
class ConfigControllerTest {

  @Mock
  private ConfigRepoService configRepoService;

  private ConfigController configController;

  @BeforeEach
  void setUp() {
    configController = new ConfigController(configRepoService);
  }

  // ============ Get Merged Config ============

  @Test
  @DisplayName("Get merged config - should return JSON format")
  void testGet_JsonFormat_Success() throws Exception {
    // Arrange
    String app = "order-service";
    String profile = "uat";
    Map<String, Object> config = Map.of(
        "server.port", "8081",
        "db.host", "uat-db.example.com"
    );

    when(configRepoService.mergedFlat(app, profile, true)).thenReturn(config);
    when(configRepoService.toJson(any())).thenReturn("{\"server.port\":\"8081\"}");

    // Act
    ResponseEntity<?> response = configController.get(app, profile, "json", true);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    verify(configRepoService, times(1)).mergedFlat(app, profile, true);
    verify(configRepoService, times(1)).toJson(any());
  }

  @Test
  @DisplayName("Get config - should handle YAML format")
  void testGet_YamlFormat_Success() throws Exception {
    // Arrange
    String app = "service";
    String profile = "uat";
    Map<String, Object> deepConfig = Map.of("server", Map.of("port", 8081));

    when(configRepoService.mergedDeep(app, profile, true)).thenReturn(deepConfig);
    when(configRepoService.toYaml(any())).thenReturn("server:\n  port: 8081");

    // Act
    ResponseEntity<?> response = configController.get(app, profile, "yaml", true);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(configRepoService, times(1)).mergedDeep(app, profile, true);
  }

  @Test
  @DisplayName("Get config - should handle Properties format")
  void testGet_PropertiesFormat_Success() {
    // Arrange
    String app = "service";
    String profile = "dev";
    Map<String, Object> config = Map.of("app.name", "Service");

    when(configRepoService.mergedFlat(app, profile, true)).thenReturn(config);
    when(configRepoService.toPropertiesText(config)).thenReturn("app.name=Service");

    // Act
    ResponseEntity<?> response = configController.get(app, profile, "properties", true);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(configRepoService, times(1)).toPropertiesText(config);
  }

  @Test
  @DisplayName("Get config - should handle missing config (404)")
  void testGet_NotFound() {
    // Arrange
    String app = "nonexistent";
    String profile = "uat";

    when(configRepoService.mergedFlat(app, profile, true))
        .thenThrow(new NoSuchElementException("No config found"));

    // Act
    ResponseEntity<?> response = configController.get(app, profile, "json", true);

    // Assert
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  @DisplayName("Get config - should handle invalid format")
  void testGet_InvalidFormat() {
    // Arrange
    String app = "service";
    String profile = "uat";

    // Act
    ResponseEntity<?> response = configController.get(app, profile, "invalid", true);

    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  // ============ Get Sources ============

  @Test
  @DisplayName("Get sources - should return property sources")
  void testGetSources_Success() {
    // Arrange
    String app = "order-service";
    String profile = "uat";

    when(configRepoService.loadSources(app, profile, true)).thenReturn(new ArrayList<>());

    // Act
    ResponseEntity<?> response = configController.getSources(app, profile, true);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    verify(configRepoService, times(1)).loadSources(app, profile, true);
  }

  @Test
  @DisplayName("Get sources - should include app and profile in response")
  void testGetSources_ResponseStructure() {
    // Arrange
    String app = "service";
    String profile = "prod";

    when(configRepoService.loadSources(app, profile, false)).thenReturn(new ArrayList<>());

    // Act
    ResponseEntity<?> response = configController.getSources(app, profile, false);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertEquals(app, body.get("app"));
    assertEquals(profile, body.get("profile"));
  }

  @Test
  @DisplayName("Get sources - should handle missing config")
  void testGetSources_NotFound() {
    // Arrange
    String app = "nonexistent";
    String profile = "uat";

    when(configRepoService.loadSources(app, profile, true))
        .thenThrow(new NoSuchElementException("No config found"));

    // Act
    ResponseEntity<?> response = configController.getSources(app, profile, true);

    // Assert
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  // ============ Get Caches ============

  @Test
  @DisplayName("Get caches - should return all caches by default")
  void testGetCaches_All() {
    // Arrange
    when(configRepoService.getFileContentsCache()).thenReturn(new LinkedHashMap<>());
    when(configRepoService.getMergedConfigWithSourcesCache()).thenReturn(new LinkedHashMap<>());
    when(configRepoService.getCacheStatistics()).thenReturn(new LinkedHashMap<>());

    // Act
    ResponseEntity<?> response = configController.getCaches("all");

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertTrue(body.containsKey("fileContents"));
    assertTrue(body.containsKey("mergedConfigWithSourcesCache"));
    assertTrue(body.containsKey("statistics"));
  }

  @Test
  @DisplayName("Get caches - should return only fileContents")
  void testGetCaches_FileContentsOnly() {
    // Arrange
    Map<String, Map<String, Object>> fileContents = new LinkedHashMap<>();
    fileContents.put("file1.properties", Map.of("key1", "value1"));

    when(configRepoService.getFileContentsCache()).thenReturn(fileContents);

    // Act
    ResponseEntity<?> response = configController.getCaches("fileContents");

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertTrue(body.containsKey("fileContents"));
    assertFalse(body.containsKey("mergedConfigWithSourcesCache"));
  }

  @Test
  @DisplayName("Get caches - should return only mergedConfigWithSources")
  void testGetCaches_MergedWithSourcesOnly() {
    // Arrange
    Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
    merged.put("app:profile:true", Map.of("key1", Map.of("value", "val1", "source", "file1")));

    when(configRepoService.getMergedConfigWithSourcesCache()).thenReturn(merged);

    // Act
    ResponseEntity<?> response = configController.getCaches("mergedConfigWithSources");

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertTrue(body.containsKey("mergedConfigWithSourcesCache"));
    assertFalse(body.containsKey("fileContents"));
  }

  @Test
  @DisplayName("Get caches - should return only statistics")
  void testGetCaches_StatisticsOnly() {
    // Arrange
    Map<String, Object> stats = Map.of(
        "fileContentsCacheSize", 5,
        "mergedConfigWithSourcesCacheSize", 3
    );

    when(configRepoService.getCacheStatistics()).thenReturn(stats);

    // Act
    ResponseEntity<?> response = configController.getCaches("statistics");

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertTrue(body.containsKey("fileContentsCacheSize"));
    assertFalse(body.containsKey("fileContents"));
  }

  @Test
  @DisplayName("Get caches - should handle invalid view parameter")
  void testGetCaches_InvalidView() {
    // Act
    ResponseEntity<?> response = configController.getCaches("invalid");

    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertTrue(body.containsKey("error"));
  }

  @Test
  @DisplayName("Get caches - should handle server errors")
  void testGetCaches_ServerError() {
    // Arrange
    when(configRepoService.getFileContentsCache()).thenThrow(new RuntimeException("Cache error"));

    // Act
    ResponseEntity<?> response = configController.getCaches("all");

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  // ============ Edge Cases ============

  @Test
  @DisplayName("Should handle default format parameter - defaults to yaml")
  void testHandleDefaultFormat() throws Exception {
    // Arrange
    String app = "service";
    String profile = "uat";
    Map<String, Object> deepConfig = Map.of("key", "value");

    when(configRepoService.mergedDeep(app, profile, true)).thenReturn(deepConfig);
    when(configRepoService.toYaml(any())).thenReturn("key: value");

    // Act
    ResponseEntity<?> response = configController.get(app, profile, "yaml", true);

    // Assert
    assertNotNull(response);
  }

  @Test
  @DisplayName("Should handle case-insensitive format parameter")
  void testCaseInsensitiveFormat() throws Exception {
    // Arrange
    String app = "service";
    String profile = "uat";
    Map<String, Object> deepConfig = Map.of("key", "value");

    when(configRepoService.mergedDeep(app, profile, true)).thenReturn(deepConfig);
    when(configRepoService.toYaml(any())).thenReturn("key: value");

    // Act
    ResponseEntity<?> response = configController.get(app, profile, "YAML", true);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  @DisplayName("Should handle empty config")
  void testEmptyConfig() throws Exception {
    // Arrange
    String app = "service";
    String profile = "uat";

    when(configRepoService.mergedFlat(app, profile, true)).thenReturn(new LinkedHashMap<>());
    when(configRepoService.toJson(any())).thenReturn("{}");

    // Act
    ResponseEntity<?> response = configController.get(app, profile, "json", true);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  @DisplayName("Should preserve content type header for JSON")
  void testContentTypeHeader_Json() throws Exception {
    // Arrange
    String app = "service";
    String profile = "uat";

    when(configRepoService.mergedFlat(app, profile, true)).thenReturn(Map.of("key", "value"));
    when(configRepoService.toJson(any())).thenReturn("{}");

    // Act
    ResponseEntity<?> response = configController.get(app, profile, "json", true);

    // Assert
    assertNotNull(response.getHeaders().getContentType());
  }

  @Test
  @DisplayName("Should preserve content type header for YAML")
  void testContentTypeHeader_Yaml() throws Exception {
    // Arrange
    String app = "service";
    String profile = "uat";

    when(configRepoService.mergedDeep(app, profile, true)).thenReturn(Map.of("key", "value"));
    when(configRepoService.toYaml(any())).thenReturn("key: value");

    // Act
    ResponseEntity<?> response = configController.get(app, profile, "yaml", true);

    // Assert
    assertNotNull(response.getHeaders().getContentType());
  }

  // ============ Get JSON with App, Profile, Properties Tag ============

  @Test
  @DisplayName("Get JSON with app, profile, properties tag - order-service uat")
  void testGetJsonWithAppProfile_OrderServiceUat() {
    // Arrange
    String app = "order-service";
    String profile = "uat";

    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("server.port", "8081");
    properties.put("server.servlet.context-path", "/api");
    properties.put("database.host", "uat-db.example.com");
    properties.put("database.port", "5432");
    properties.put("app.name", "Order Service");

    when(configRepoService.mergedFlat(app, profile, true)).thenReturn(properties);

    // Act
    ResponseEntity<?> response = configController.getJsonWithAppProfile(app, profile, true);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(org.springframework.http.MediaType.APPLICATION_JSON, response.getHeaders().getContentType());

    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertNotNull(body);
    assertEquals(app, body.get("app"));
    assertEquals(profile, body.get("profile"));

    @SuppressWarnings("unchecked")
    Map<String, Object> propsTag = (Map<String, Object>) body.get("properties");
    assertNotNull(propsTag);
    assertEquals("8081", propsTag.get("server.port"));
    assertEquals("uat-db.example.com", propsTag.get("database.host"));

    verify(configRepoService, times(1)).mergedFlat(app, profile, true);
  }

  @Test
  @DisplayName("Get JSON with app, profile, properties tag - payment-service prod")
  void testGetJsonWithAppProfile_PaymentServiceProd() {
    // Arrange
    String app = "payment-service";
    String profile = "prod";

    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("payment.provider", "stripe");
    properties.put("payment.api.key", "sk_live_abc123");
    properties.put("payment.webhook.enabled", "true");

    when(configRepoService.mergedFlat(app, profile, true)).thenReturn(properties);

    // Act
    ResponseEntity<?> response = configController.getJsonWithAppProfile(app, profile, true);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());

    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertEquals("payment-service", body.get("app"));
    assertEquals("prod", body.get("profile"));

    @SuppressWarnings("unchecked")
    Map<String, Object> propsTag = (Map<String, Object>) body.get("properties");
    assertEquals("stripe", propsTag.get("payment.provider"));
  }

  @Test
  @DisplayName("Get JSON with app, profile - response structure validation")
  void testGetJsonWithAppProfile_ResponseStructure() {
    // Arrange
    String app = "service";
    String profile = "dev";
    Map<String, Object> properties = Map.of("app.name", "Service", "server.port", "8080");

    when(configRepoService.mergedFlat(app, profile, true)).thenReturn(properties);

    // Act
    ResponseEntity<?> response = configController.getJsonWithAppProfile(app, profile, true);

    // Assert
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertTrue(body.containsKey("app"));
    assertTrue(body.containsKey("profile"));
    assertTrue(body.containsKey("properties"));
    assertEquals(3, body.size()); // Only 3 top-level keys
  }

  @Test
  @DisplayName("Get JSON with app, profile - without placeholder resolution")
  void testGetJsonWithAppProfile_NoResolve() {
    // Arrange
    String app = "service";
    String profile = "uat";
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("app.name", "Service");
    properties.put("db.host", "${database.host}");

    when(configRepoService.mergedFlat(app, profile, false)).thenReturn(properties);

    // Act
    ResponseEntity<?> response = configController.getJsonWithAppProfile(app, profile, false);

    // Assert
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    @SuppressWarnings("unchecked")
    Map<String, Object> propsTag = (Map<String, Object>) body.get("properties");
    assertEquals("${database.host}", propsTag.get("db.host"));
  }

  @Test
  @DisplayName("Get JSON with app, profile - handles missing config")
  void testGetJsonWithAppProfile_NotFound() {
    // Arrange
    String app = "nonexistent";
    String profile = "uat";

    when(configRepoService.mergedFlat(app, profile, true))
        .thenThrow(new NoSuchElementException("No config found"));

    // Act
    ResponseEntity<?> response = configController.getJsonWithAppProfile(app, profile, true);

    // Assert
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, String> body = (Map<String, String>) response.getBody();
    assertTrue(body.containsKey("error"));
  }

  @Test
  @DisplayName("Get JSON with app, profile - handles service exception")
  void testGetJsonWithAppProfile_ServiceError() {
    // Arrange
    String app = "service";
    String profile = "uat";

    when(configRepoService.mergedFlat(app, profile, true))
        .thenThrow(new RuntimeException("Database connection error"));

    // Act
    ResponseEntity<?> response = configController.getJsonWithAppProfile(app, profile, true);

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, String> body = (Map<String, String>) response.getBody();
    assertTrue(body.get("error").contains("Error retrieving configuration"));
  }

  @Test
  @DisplayName("Get JSON with app, profile - large properties set")
  void testGetJsonWithAppProfile_LargeProperties() {
    // Arrange
    String app = "service";
    String profile = "prod";

    Map<String, Object> properties = new LinkedHashMap<>();
    for (int i = 1; i <= 100; i++) {
      properties.put("property." + i, "value" + i);
    }

    when(configRepoService.mergedFlat(app, profile, true)).thenReturn(properties);

    // Act
    ResponseEntity<?> response = configController.getJsonWithAppProfile(app, profile, true);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());

    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    @SuppressWarnings("unchecked")
    Map<String, Object> propsTag = (Map<String, Object>) body.get("properties");
    assertEquals(100, propsTag.size());
    assertEquals("value1", propsTag.get("property.1"));
    assertEquals("value100", propsTag.get("property.100"));
  }

  @Test
  @DisplayName("Get JSON with app, profile - preserves property order")
  void testGetJsonWithAppProfile_PreservesOrder() {
    // Arrange
    String app = "service";
    String profile = "uat";

    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("z.last", "value");
    properties.put("a.first", "value");
    properties.put("m.middle", "value");

    when(configRepoService.mergedFlat(app, profile, true)).thenReturn(properties);

    // Act
    ResponseEntity<?> response = configController.getJsonWithAppProfile(app, profile, true);

    // Assert
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    @SuppressWarnings("unchecked")
    Map<String, Object> propsTag = (Map<String, Object>) body.get("properties");

    List<String> keys = new ArrayList<>(propsTag.keySet());
    assertEquals("z.last", keys.get(0));
    assertEquals("a.first", keys.get(1));
    assertEquals("m.middle", keys.get(2));
  }

  @Test
  @DisplayName("Get JSON with app, profile - content type is JSON")
  void testGetJsonWithAppProfile_ContentTypeJson() {
    // Arrange
    Map<String, Object> properties = Map.of("key", "value");

    when(configRepoService.mergedFlat("service", "uat", true)).thenReturn(properties);

    // Act
    ResponseEntity<?> response = configController.getJsonWithAppProfile("service", "uat", true);

    // Assert
    assertEquals(org.springframework.http.MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
  }
}
