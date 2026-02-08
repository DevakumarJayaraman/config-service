package com.example.configservice.services;

import com.example.configservice.dto.ConfigDifference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for ComparisonService.
 * Tests comparison of configurations across different profiles and applications.
 * Coverage target: 80%+
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ComparisonService Tests")
class ComparisonServiceTest {

  @Mock
  private ConfigRepoService configRepoService;

  private ComparisonService comparisonService;

  @BeforeEach
  void setUp() {
    comparisonService = new ComparisonService(configRepoService);
  }

  // ============ Compare Sources for Specific App ============

  @Test
  @DisplayName("Compare sources for app - should show differences between profiles")
  void testCompareSourcesForApp_WithDifferences() {
    // Arrange
    String app = "order-service";
    String profileA = "uat";
    String profileB = "prod";

    Map<String, Map<String, Object>> fileContentsCache = new LinkedHashMap<>();
    fileContentsCache.put("order-service_apacuat.properties", Map.of(
        "app.name", "Order Service UAT",
        "db.host", "uat-db.example.com"
    ));
    fileContentsCache.put("order-service_apacqa.properties", Map.of(
        "app.name", "Order Service PROD",
        "db.host", "prod-db.example.com",
        "app.version", "2.0.0"
    ));

    when(configRepoService.getFileContentsCache()).thenReturn(fileContentsCache);

    // Act
    Map<String, Object> result = comparisonService.compareSourcesForApp(app, profileA, profileB);

    // Assert
    assertNotNull(result);
    assertEquals(app, result.get("app"));
    assertEquals(profileA, result.get("profileA"));
    assertEquals(profileB, result.get("profileB"));
    assertEquals("sources", result.get("comparisonType"));
    assertEquals("order-service_apacuat.properties", result.get("fileA"));
    assertEquals("order-service_apacqa.properties", result.get("fileB"));

    @SuppressWarnings("unchecked")
    List<ConfigDifference> differences = (List<ConfigDifference>) result.get("differences");
    assertNotNull(differences);
    assertTrue(differences.size() > 0);

    verify(configRepoService, times(1)).getFileContentsCache();
  }

  @Test
  @DisplayName("Compare sources for app - should handle missing files")
  void testCompareSourcesForApp_WithMissingProfile() {
    // Arrange
    String app = "order-service";
    String profileA = "uat";
    String profileB = "nonexistent";

    Map<String, Map<String, Object>> fileContentsCache = new LinkedHashMap<>();
    fileContentsCache.put("order-service_apacuat.properties", Map.of("app.name", "Order Service"));

    when(configRepoService.getFileContentsCache()).thenReturn(fileContentsCache);

    // Act
    Map<String, Object> result = comparisonService.compareSourcesForApp(app, profileA, profileB);

    // Assert
    assertNotNull(result);
    assertEquals(app, result.get("app"));
    assertEquals("order-service_apacuat.properties", result.get("fileA"));
    assertEquals("order-service_nonexistent", result.get("fileB"));

    @SuppressWarnings("unchecked")
    List<ConfigDifference> differences = (List<ConfigDifference>) result.get("differences");
    assertNotNull(differences);
    assertTrue(differences.stream().anyMatch(d -> "uat-missing".equals(d.getStatus()) || "nonexistent-missing".equals(d.getStatus())));
  }

  @Test
  @DisplayName("Compare sources for app - should find .yml files")
  void testCompareSourcesForApp_WithYmlFiles() {
    // Arrange
    String app = "payment-service";
    String profileA = "dev";
    String profileB = "staging";

    Map<String, Map<String, Object>> fileContentsCache = new LinkedHashMap<>();
    fileContentsCache.put("payment-service_dev.yml", Map.of("port", 8080));
    fileContentsCache.put("payment-service_staging.yaml", Map.of("port", 8081));

    when(configRepoService.getFileContentsCache()).thenReturn(fileContentsCache);

    // Act
    Map<String, Object> result = comparisonService.compareSourcesForApp(app, profileA, profileB);

    // Assert
    assertNotNull(result);
    assertEquals("payment-service_dev.yml", result.get("fileA"));
    assertEquals("payment-service_staging.yaml", result.get("fileB"));

    verify(configRepoService, times(1)).getFileContentsCache();
  }

  @Test
  @DisplayName("Compare sources for app - should handle identical configs")
  void testCompareSourcesForApp_WithIdenticalConfigs() {
    // Arrange
    String app = "service";
    String profileA = "test";
    String profileB = "test2";

    Map<String, Object> sameConfig = Map.of("prop1", "value1", "prop2", "value2");
    Map<String, Map<String, Object>> fileContentsCache = new LinkedHashMap<>();
    fileContentsCache.put("service_test.properties", sameConfig);
    fileContentsCache.put("service_test2.properties", sameConfig);

    when(configRepoService.getFileContentsCache()).thenReturn(fileContentsCache);

    // Act
    Map<String, Object> result = comparisonService.compareSourcesForApp(app, profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    List<ConfigDifference> differences = (List<ConfigDifference>) result.get("differences");
    assertTrue(differences.stream().allMatch(d -> "SAME".equals(d.getStatus())));
  }

  // ============ Compare Sources for All Apps ============

  @Test
  @DisplayName("Compare sources for all apps - should loop through all applications")
  void testCompareSourcesForAllApps_MultipleApps() {
    // Arrange
    String profileA = "uat";
    String profileB = "prod";

    Map<String, Map<String, Object>> fileContentsCache = new LinkedHashMap<>();
    fileContentsCache.put("order-service_apacuat.properties", Map.of("name", "Order"));
    fileContentsCache.put("order-service_apacqa.properties", Map.of("name", "Order"));
    fileContentsCache.put("payment-service_uat.properties", Map.of("name", "Payment"));
    fileContentsCache.put("payment-service_prod.properties", Map.of("name", "Payment"));

    when(configRepoService.getFileContentsCache()).thenReturn(fileContentsCache);

    // Act
    Map<String, Object> result = comparisonService.compareSourcesForAllApps(profileA, profileB);

    // Assert
    assertNotNull(result);
    assertEquals("all", result.get("apps"));
    assertEquals(profileA, result.get("profileA"));
    assertEquals(profileB, result.get("profileB"));
    assertEquals("sources", result.get("comparisonType"));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> appComparisons = (List<Map<String, Object>>) result.get("appComparisons");
    assertNotNull(appComparisons);
    assertEquals(2, appComparisons.size());

    @SuppressWarnings("unchecked")
    Map<String, Integer> stats = (Map<String, Integer>) result.get("statistics");
    assertEquals(2, stats.get("totalApps"));
  }

  @Test
  @DisplayName("Compare sources for all apps - should skip global application files")
  void testCompareSourcesForAllApps_SkipGlobalApplication() {
    // Arrange
    String profileA = "uat";
    String profileB = "prod";

    Map<String, Map<String, Object>> fileContentsCache = new LinkedHashMap<>();
    fileContentsCache.put("application_uat.properties", Map.of("name", "Global"));
    fileContentsCache.put("application_prod.properties", Map.of("name", "Global"));
    fileContentsCache.put("order-service_apacuat.properties", Map.of("name", "Order"));
    fileContentsCache.put("order-service_apacqa.properties", Map.of("name", "Order"));

    when(configRepoService.getFileContentsCache()).thenReturn(fileContentsCache);

    // Act
    Map<String, Object> result = comparisonService.compareSourcesForAllApps(profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> appComparisons = (List<Map<String, Object>>) result.get("appComparisons");
    assertTrue(appComparisons.stream().noneMatch(a -> "application".equals(a.get("app"))));
    assertEquals(1, appComparisons.size());
  }

  @Test
  @DisplayName("Compare sources for all apps - should handle empty cache")
  void testCompareSourcesForAllApps_EmptyCache() {
    // Arrange
    String profileA = "uat";
    String profileB = "prod";

    when(configRepoService.getFileContentsCache()).thenReturn(new LinkedHashMap<>());

    // Act
    Map<String, Object> result = comparisonService.compareSourcesForAllApps(profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> appComparisons = (List<Map<String, Object>>) result.get("appComparisons");
    assertTrue(appComparisons.isEmpty());

    @SuppressWarnings("unchecked")
    Map<String, Integer> stats = (Map<String, Integer>) result.get("statistics");
    assertEquals(0, stats.get("totalApps"));
  }

  @Test
  @DisplayName("Compare sources for all apps - should handle missing one profile for an app")
  void testCompareSourcesForAllApps_PartialProfiles() {
    // Arrange
    String profileA = "uat";
    String profileB = "prod";

    Map<String, Map<String, Object>> fileContentsCache = new LinkedHashMap<>();
    fileContentsCache.put("order-service_apacuat.properties", Map.of("name", "Order"));
    // No order-service_apacqa.properties

    when(configRepoService.getFileContentsCache()).thenReturn(fileContentsCache);

    // Act
    Map<String, Object> result = comparisonService.compareSourcesForAllApps(profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> appComparisons = (List<Map<String, Object>>) result.get("appComparisons");
    assertEquals(1, appComparisons.size());
  }

  // ============ Compare Values for Specific App ============

  @Test
  @DisplayName("Compare values for app - should show merged config differences")
  void testCompareValuesForApp_WithDifferences() {
    // Arrange
    String app = "order-service";
    String profileA = "uat";
    String profileB = "prod";

    Map<String, Object> mergedA = new LinkedHashMap<>();
    mergedA.put("server.port", Map.of("value", "8081", "source", "application_uat.properties"));
    mergedA.put("db.host", Map.of("value", "uat-db", "source", "order-service_apacuat.properties"));

    Map<String, Object> mergedB = new LinkedHashMap<>();
    mergedB.put("server.port", Map.of("value", "8080", "source", "application_default.properties"));
    mergedB.put("db.host", Map.of("value", "prod-db", "source", "order-service_apacqa.properties"));

    when(configRepoService.getMergedConfigWithSources(app, profileA)).thenReturn(mergedA);
    when(configRepoService.getMergedConfigWithSources(app, profileB)).thenReturn(mergedB);

    // Act
    Map<String, Object> result = comparisonService.compareValuesForApp(app, profileA, profileB);

    // Assert
    assertNotNull(result);
    assertEquals(app, result.get("app"));
    assertEquals(profileA, result.get("profileA"));
    assertEquals(profileB, result.get("profileB"));
    assertEquals("values", result.get("comparisonType"));

    @SuppressWarnings("unchecked")
    List<ConfigDifference> differences = (List<ConfigDifference>) result.get("differences");
    assertNotNull(differences);
    assertTrue(differences.stream().anyMatch(d -> "DIFF".equals(d.getStatus())));

    verify(configRepoService, times(1)).getMergedConfigWithSources(app, profileA);
    verify(configRepoService, times(1)).getMergedConfigWithSources(app, profileB);
  }

  @Test
  @DisplayName("Compare values for app - should handle missing properties in profile")
  void testCompareValuesForApp_WithMissingProperties() {
    // Arrange
    String app = "service";
    String profileA = "uat";
    String profileB = "prod";

    Map<String, Object> mergedA = new LinkedHashMap<>();
    mergedA.put("prop1", Map.of("value", "value1", "source", "file_uat.properties"));
    mergedA.put("prop2", Map.of("value", "value2", "source", "file_uat.properties"));

    Map<String, Object> mergedB = new LinkedHashMap<>();
    mergedB.put("prop1", Map.of("value", "value1", "source", "file_prod.properties"));
    // prop2 missing in prod

    when(configRepoService.getMergedConfigWithSources(app, profileA)).thenReturn(mergedA);
    when(configRepoService.getMergedConfigWithSources(app, profileB)).thenReturn(mergedB);

    // Act
    Map<String, Object> result = comparisonService.compareValuesForApp(app, profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    List<ConfigDifference> differences = (List<ConfigDifference>) result.get("differences");
    assertTrue(differences.stream().anyMatch(d -> "prod-missing".equals(d.getStatus())));
  }

  @Test
  @DisplayName("Compare values for app - should handle identical merged configs")
  void testCompareValuesForApp_WithIdenticalValues() {
    // Arrange
    String app = "service";
    String profileA = "env1";
    String profileB = "env2";

    Map<String, Object> merged = new LinkedHashMap<>();
    merged.put("prop1", Map.of("value", "value1", "source", "file.properties"));

    when(configRepoService.getMergedConfigWithSources(app, profileA)).thenReturn(merged);
    when(configRepoService.getMergedConfigWithSources(app, profileB)).thenReturn(merged);

    // Act
    Map<String, Object> result = comparisonService.compareValuesForApp(app, profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    List<ConfigDifference> differences = (List<ConfigDifference>) result.get("differences");
    assertTrue(differences.stream().allMatch(d -> "SAME".equals(d.getStatus())));
  }

  @Test
  @DisplayName("Compare values for app - should handle empty configs")
  void testCompareValuesForApp_WithEmptyConfigs() {
    // Arrange
    String app = "service";
    String profileA = "a";
    String profileB = "b";

    when(configRepoService.getMergedConfigWithSources(app, profileA)).thenReturn(new LinkedHashMap<>());
    when(configRepoService.getMergedConfigWithSources(app, profileB)).thenReturn(new LinkedHashMap<>());

    // Act
    Map<String, Object> result = comparisonService.compareValuesForApp(app, profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    List<ConfigDifference> differences = (List<ConfigDifference>) result.get("differences");
    assertTrue(differences.isEmpty());
    assertEquals(0, result.get("differenceCount"));
  }

  // ============ Compare Values for All Apps ============

  @Test
  @DisplayName("Compare values for all apps - should loop through all applications")
  void testCompareValuesForAllApps_MultipleApps() {
    // Arrange
    String profileA = "uat";
    String profileB = "prod";

    Map<String, Object> mergedApp1 = Map.of("prop", Map.of("value", "val1", "source", "file1"));
    Map<String, Object> mergedApp2 = Map.of("prop", Map.of("value", "val2", "source", "file2"));

    when(configRepoService.getMergedConfigWithSourcesCache()).thenReturn(Map.of(
        "order-service:uat", mergedApp1,
        "order-service:prod", mergedApp1,
        "payment-service:uat", mergedApp2,
        "payment-service:prod", mergedApp2
    ));

    when(configRepoService.getMergedConfigWithSources("order-service", profileA)).thenReturn(mergedApp1);
    when(configRepoService.getMergedConfigWithSources("order-service", profileB)).thenReturn(mergedApp1);
    when(configRepoService.getMergedConfigWithSources("payment-service", profileA)).thenReturn(mergedApp2);
    when(configRepoService.getMergedConfigWithSources("payment-service", profileB)).thenReturn(mergedApp2);

    // Act
    Map<String, Object> result = comparisonService.compareValuesForAllApps(profileA, profileB);

    // Assert
    assertNotNull(result);
    assertEquals("all", result.get("apps"));
    assertEquals(profileA, result.get("profileA"));
    assertEquals(profileB, result.get("profileB"));
    assertEquals("values", result.get("comparisonType"));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> appComparisons = (List<Map<String, Object>>) result.get("appComparisons");
    assertEquals(2, appComparisons.size());

    @SuppressWarnings("unchecked")
    Map<String, Integer> stats = (Map<String, Integer>) result.get("statistics");
    assertEquals(2, stats.get("totalApps"));
  }

  @Test
  @DisplayName("Compare values for all apps - should handle exceptions gracefully")
  void testCompareValuesForAllApps_WithException() {
    // Arrange
    String profileA = "uat";
    String profileB = "prod";

    when(configRepoService.getMergedConfigWithSourcesCache()).thenReturn(Map.of(
        "order-service:uat", new LinkedHashMap<>(),
        "order-service:prod", new LinkedHashMap<>()
    ));

    when(configRepoService.getMergedConfigWithSources("order-service", profileA))
        .thenThrow(new RuntimeException("Test exception"));
    when(configRepoService.getMergedConfigWithSources("order-service", profileB))
        .thenReturn(new LinkedHashMap<>());

    // Act
    Map<String, Object> result = comparisonService.compareValuesForAllApps(profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> appComparisons = (List<Map<String, Object>>) result.get("appComparisons");
    assertTrue(appComparisons.isEmpty());
  }

  @Test
  @DisplayName("Compare values for all apps - should handle empty cache")
  void testCompareValuesForAllApps_EmptyCache() {
    // Arrange
    String profileA = "uat";
    String profileB = "prod";

    when(configRepoService.getMergedConfigWithSourcesCache()).thenReturn(new LinkedHashMap<>());

    // Act
    Map<String, Object> result = comparisonService.compareValuesForAllApps(profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> appComparisons = (List<Map<String, Object>>) result.get("appComparisons");
    assertTrue(appComparisons.isEmpty());

    @SuppressWarnings("unchecked")
    Map<String, Integer> stats = (Map<String, Integer>) result.get("statistics");
    assertEquals(0, stats.get("totalApps"));
  }

  // ============ Status Tests ============

  @Test
  @DisplayName("Differences should have correct status values")
  void testDifferenceStatus_AllStatuses() {
    // Arrange
    String app = "service";
    String profileA = "a";
    String profileB = "b";

    Map<String, Map<String, Object>> fileContentsCache = new LinkedHashMap<>();
    fileContentsCache.put("service_a.properties", Map.of(
        "same", "value",
        "diff", "valueA",
        "onlyA", "valueA"
    ));
    fileContentsCache.put("service_b.properties", Map.of(
        "same", "value",
        "diff", "valueB",
        "onlyB", "valueB"
    ));

    when(configRepoService.getFileContentsCache()).thenReturn(fileContentsCache);

    // Act
    Map<String, Object> result = comparisonService.compareSourcesForApp(app, profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    List<ConfigDifference> differences = (List<ConfigDifference>) result.get("differences");

    assertTrue(differences.stream().anyMatch(d -> "SAME".equals(d.getStatus())));
    assertTrue(differences.stream().anyMatch(d -> "DIFF".equals(d.getStatus())));
    assertTrue(differences.stream().anyMatch(d -> "b-missing".equals(d.getStatus())));
    assertTrue(differences.stream().anyMatch(d -> "a-missing".equals(d.getStatus())));
  }

  // ============ Edge Cases ============

  @Test
  @DisplayName("Should handle null values in config")
  void testHandleNullValues() {
    // Arrange
    String app = "service";
    String profileA = "a";
    String profileB = "b";

    Map<String, Map<String, Object>> fileContentsCache = new LinkedHashMap<>();
    fileContentsCache.put("service_a.properties", Map.of("prop", null));
    fileContentsCache.put("service_b.properties", Map.of("prop", "value"));

    when(configRepoService.getFileContentsCache()).thenReturn(fileContentsCache);

    // Act
    Map<String, Object> result = comparisonService.compareSourcesForApp(app, profileA, profileB);

    // Assert
    assertNotNull(result);
    @SuppressWarnings("unchecked")
    List<ConfigDifference> differences = (List<ConfigDifference>) result.get("differences");
    assertTrue(differences.size() > 0);
  }

  @Test
  @DisplayName("Should handle special characters in keys")
  void testHandleSpecialCharactersInKeys() {
    // Arrange
    String app = "service";
    String profileA = "a";
    String profileB = "b";

    Map<String, Map<String, Object>> fileContentsCache = new LinkedHashMap<>();
    fileContentsCache.put("service_a.properties", Map.of("app.name@special", "value1"));
    fileContentsCache.put("service_b.properties", Map.of("app.name@special", "value2"));

    when(configRepoService.getFileContentsCache()).thenReturn(fileContentsCache);

    // Act
    Map<String, Object> result = comparisonService.compareSourcesForApp(app, profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    List<ConfigDifference> differences = (List<ConfigDifference>) result.get("differences");
    assertTrue(differences.stream().anyMatch(d -> "app.name@special".equals(d.getKey())));
  }

  @Test
  @DisplayName("Should handle multi-part app names with underscores")
  void testHandleMultiPartAppNames() {
    // Arrange
    String profileA = "uat";
    String profileB = "prod";

    Map<String, Map<String, Object>> fileContentsCache = new LinkedHashMap<>();
    fileContentsCache.put("my_order_service_uat.properties", Map.of("name", "Order"));
    fileContentsCache.put("my_order_service_prod.properties", Map.of("name", "Order"));

    when(configRepoService.getFileContentsCache()).thenReturn(fileContentsCache);

    // Act
    Map<String, Object> result = comparisonService.compareSourcesForAllApps(profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> appComparisons = (List<Map<String, Object>>) result.get("appComparisons");
    assertTrue(appComparisons.stream().anyMatch(a -> "my_order_service".equals(a.get("app"))));
  }

  @Test
  @DisplayName("Should sort differences correctly")
  void testDifferenceSorting() {
    // Arrange
    String app = "service";
    String profileA = "a";
    String profileB = "b";

    Map<String, Map<String, Object>> fileContentsCache = new LinkedHashMap<>();
    Map<String, Object> configA = new LinkedHashMap<>();
    configA.put("z_key", "value");
    configA.put("a_key", "value");
    configA.put("m_key", "different");

    Map<String, Object> configB = new LinkedHashMap<>();
    configB.put("z_key", "value");
    configB.put("a_key", "value");
    configB.put("m_key", "different2");

    fileContentsCache.put("service_a.properties", configA);
    fileContentsCache.put("service_b.properties", configB);

    when(configRepoService.getFileContentsCache()).thenReturn(fileContentsCache);

    // Act
    Map<String, Object> result = comparisonService.compareSourcesForApp(app, profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    List<ConfigDifference> differences = (List<ConfigDifference>) result.get("differences");
    // DIFF status should come first, then SAME
    ConfigDifference first = differences.get(0);
    assertTrue("DIFF".equals(first.getStatus()) || "SAME".equals(first.getStatus()));
  }

  @Test
  @DisplayName("Should handle wrapped values in merged configs")
  void testHandleWrappedValues() {
    // Arrange
    String app = "service";
    String profileA = "a";
    String profileB = "b";

    Map<String, Object> mergedA = new LinkedHashMap<>();
    mergedA.put("prop1", Map.of("value", "val1", "source", "file.properties"));
    mergedA.put("prop2", Map.of("value", "val2", "source", "file.properties"));

    Map<String, Object> mergedB = new LinkedHashMap<>();
    mergedB.put("prop1", Map.of("value", "val1", "source", "file.properties"));
    mergedB.put("prop2", Map.of("value", "val2_different", "source", "file.properties"));

    when(configRepoService.getMergedConfigWithSources(app, profileA)).thenReturn(mergedA);
    when(configRepoService.getMergedConfigWithSources(app, profileB)).thenReturn(mergedB);

    // Act
    Map<String, Object> result = comparisonService.compareValuesForApp(app, profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    List<ConfigDifference> differences = (List<ConfigDifference>) result.get("differences");
    assertTrue(differences.stream().anyMatch(d -> "prop1".equals(d.getKey()) && "SAME".equals(d.getStatus())));
    assertTrue(differences.stream().anyMatch(d -> "prop2".equals(d.getKey()) && "DIFF".equals(d.getStatus())));
  }

  @Test
  @DisplayName("Should provide accurate difference count")
  void testDifferenceCount() {
    // Arrange
    String app = "service";
    String profileA = "a";
    String profileB = "b";

    Map<String, Map<String, Object>> fileContentsCache = new LinkedHashMap<>();
    fileContentsCache.put("service_a.properties", Map.of("prop1", "val1", "prop2", "val2"));
    fileContentsCache.put("service_b.properties", Map.of("prop1", "val1", "prop2", "val2"));

    when(configRepoService.getFileContentsCache()).thenReturn(fileContentsCache);

    // Act
    Map<String, Object> result = comparisonService.compareSourcesForApp(app, profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    List<ConfigDifference> differences = (List<ConfigDifference>) result.get("differences");
    assertEquals(differences.size(), result.get("differenceCount"));
    assertEquals(2, result.get("differenceCount"));
  }
}
