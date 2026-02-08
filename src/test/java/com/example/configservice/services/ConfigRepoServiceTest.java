package com.example.configservice.services;

import com.example.configservice.config.ConfigRepoProperties;
import com.example.configservice.dto.ConfigSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for ConfigRepoService.
 * Tests configuration caching, merging, and retrieval.
 * Note: Full integration tests excluded to focus on core logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigRepoService Tests")
class ConfigRepoServiceTest {

  @Mock
  private ConfigRepoProperties configRepoProperties;

  @Mock
  private Environment environment;

  private ConfigRepoService configRepoService;

  @BeforeEach
  void setUp() {
    configRepoService = new ConfigRepoService(configRepoProperties, environment);
  }

  // ============ Cache Access ============

  @Test
  @DisplayName("Should return fileContents cache")
  void testGetFileContentsCache_Success() {
    // Act
    Map<String, Map<String, Object>> cache = configRepoService.getFileContentsCache();

    // Assert
    assertNotNull(cache);
    assertFalse(cache.isEmpty()); // Should have caches initialized
  }

  @Test
  @DisplayName("Should return mergedConfigWithSourcesCache")
  void testGetMergedConfigWithSourcesCache_Success() {
    // Act
    Map<String, Map<String, Object>> cache = configRepoService.getMergedConfigWithSourcesCache();

    // Assert
    assertNotNull(cache);
  }

  @Test
  @DisplayName("Should return cache statistics")
  void testGetCacheStatistics_Success() {
    // Act
    Map<String, Object> stats = configRepoService.getCacheStatistics();

    // Assert
    assertNotNull(stats);
    assertTrue(stats.containsKey("fileContentsCacheSize"));
    assertTrue(stats.containsKey("mergedConfigWithSourcesCacheSize"));
    assertTrue(stats.containsKey("totalCacheEntries"));
  }

  @Test
  @DisplayName("Should return accurate cache sizes in statistics")
  void testGetCacheStatistics_AccurateSizes() {
    // Act
    Map<String, Object> stats = configRepoService.getCacheStatistics();

    // Assert
    Number fileSize = (Number) stats.get("fileContentsCacheSize");
    Number mergedSize = (Number) stats.get("mergedConfigWithSourcesCacheSize");
    Number totalSize = (Number) stats.get("totalCacheEntries");

    assertNotNull(fileSize);
    assertNotNull(mergedSize);
    assertNotNull(totalSize);
    assertTrue(totalSize.intValue() >= fileSize.intValue());
  }

  // ============ Merged Config Retrieval ============

  @Test
  @DisplayName("Should retrieve merged flat config")
  void testMergedFlat_Success() {
    // Arrange
    String app = "service";
    String profile = "uat";

    // Act - Should not throw
    assertDoesNotThrow(() -> {
      Map<String, Object> config = configRepoService.mergedFlat(app, profile, true);
      // Assert
      assertNotNull(config);
    });
  }

  @Test
  @DisplayName("Should retrieve merged deep config")
  void testMergedDeep_Success() {
    // Arrange
    String app = "service";
    String profile = "prod";

    // Act - Should not throw
    assertDoesNotThrow(() -> {
      Map<String, Object> config = configRepoService.mergedDeep(app, profile, true);
      // Assert
      assertNotNull(config);
    });
  }

  @Test
  @DisplayName("Should throw on non-existent app-profile combo")
  void testMergedFlat_NotFound() {
    // Act & Assert
    assertThrows(NoSuchElementException.class, () -> {
      configRepoService.mergedFlat("nonexistent_app_12345", "nonexistent_profile_12345", true);
    });
  }

  // ============ Source Tracking ============

  @Test
  @DisplayName("Should get sources for config")
  void testGetSourcesForConfig_Success() {
    // Arrange
    String app = "service";
    String profile = "uat";

    // Act
    Map<String, String> sources = configRepoService.getSourcesForConfig(app, profile);

    // Assert
    assertNotNull(sources);
  }

  @Test
  @DisplayName("Should track actual source filenames")
  void testGetSourcesForConfig_TrackFilenames() {
    // Arrange
    String app = "service";
    String profile = "uat";

    // Act
    Map<String, String> sources = configRepoService.getSourcesForConfig(app, profile);

    // Assert
    // All source values should be filenames (contain . for extension)
    for (String source : sources.values()) {
      if (!source.equals("NOT_FOUND")) {
        assertTrue(source.contains(".") || source.contains("_"),
            "Source should be a filename: " + source);
      }
    }
  }

  // ============ Load Sources ============

  @Test
  @DisplayName("Should load sources for app-profile")
  void testLoadSources_Success() {
    // Arrange
    String app = "service";
    String profile = "uat";

    // Act
    List<ConfigSource> sources = configRepoService.loadSources(app, profile, true);

    // Assert
    assertNotNull(sources);
  }

  @Test
  @DisplayName("Should throw on non-existent sources")
  void testLoadSources_NotFound() {
    // Act & Assert
    assertThrows(NoSuchElementException.class, () -> {
      configRepoService.loadSources("nonexistent_app_xyz", "nonexistent_profile_xyz", true);
    });
  }

  // ============ Format Conversion ============

  @Test
  @DisplayName("Should convert to JSON format")
  void testToJson_Success() throws Exception {
    // Arrange
    Map<String, Object> data = Map.of("key", "value");

    // Act
    String json = configRepoService.toJson(data);

    // Assert
    assertNotNull(json);
    assertTrue(json.contains("key"));
    assertTrue(json.contains("value"));
  }

  @Test
  @DisplayName("Should convert to YAML format")
  void testToYaml_Success() throws Exception {
    // Arrange
    Map<String, Object> data = Map.of("port", 8080);

    // Act
    String yaml = configRepoService.toYaml(data);

    // Assert
    assertNotNull(yaml);
  }

  @Test
  @DisplayName("Should convert to Properties format")
  void testToPropertiesText_Success() {
    // Arrange
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("app.name", "Service");
    config.put("app.version", "1.0.0");

    // Act
    String props = configRepoService.toPropertiesText(config);

    // Assert
    assertNotNull(props);
    assertTrue(props.contains("app.name"));
    assertTrue(props.contains("app.version"));
  }

  // ============ Cache Management ============

  @Test
  @DisplayName("Should clear caches")
  void testClearCache_Success() {
    // Act & Assert
    assertDoesNotThrow(() -> configRepoService.clearCache());
  }

  @Test
  @DisplayName("Should reinitialize caches")
  void testInitializeCaches_Success() {
    // Act & Assert
    assertDoesNotThrow(() -> configRepoService.initializeCaches());
  }

  // ============ Get Individual File Contents ============

  @Test
  @DisplayName("Should get file contents from cache")
  void testGetFileContents_Success() {
    // Arrange
    String filename = "test.properties";

    // Act
    Map<String, Object> contents = configRepoService.getFileContents(filename);

    // Assert
    assertNotNull(contents);
    // Will be empty if file doesn't exist, which is OK
  }

  // ============ Get Merged Config With Sources ============

  @Test
  @DisplayName("Should get merged config with source tracking")
  void testGetMergedConfigWithSources_Success() {
    // Arrange
    String app = "service";
    String profile = "uat";

    // Act
    Map<String, Object> config = configRepoService.getMergedConfigWithSources(app, profile);

    // Assert
    assertNotNull(config);
  }

  // ============ Edge Cases ============

  @Test
  @DisplayName("Should handle empty app name")
  void testEmptyAppName() {
    // Act & Assert
    assertThrows(NoSuchElementException.class, () -> {
      configRepoService.mergedFlat("", "uat", true);
    });
  }

  @Test
  @DisplayName("Should handle empty profile")
  void testEmptyProfile() {
    // Act & Assert
    assertThrows(NoSuchElementException.class, () -> {
      configRepoService.mergedFlat("service", "", true);
    });
  }

  @Test
  @DisplayName("Should handle special characters in app name")
  void testSpecialCharactersInAppName() {
    // Arrange
    String app = "my-service@2.0";
    String profile = "uat";

    // Act & Assert
    assertThrows(NoSuchElementException.class, () -> {
      configRepoService.mergedFlat(app, profile, true);
    });
  }

  @Test
  @DisplayName("Should preserve order in config")
  void testConfigOrder_Preserved() {
    // Arrange
    String app = "service";
    String profile = "uat";

    // Act
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("z_last", "value");
    config.put("a_first", "value");
    config.put("m_middle", "value");

    // Assert - LinkedHashMap should preserve insertion order
    String propsText = configRepoService.toPropertiesText(config);
    int zPos = propsText.indexOf("z_last");
    int aPos = propsText.indexOf("a_first");
    assertTrue(zPos < aPos, "Order should be preserved");
  }

  @Test
  @DisplayName("Should handle null values in config")
  void testNullValuesInConfig() {
    // Arrange
    Map<String, Object> config = Map.of("key", null);

    // Act
    String props = configRepoService.toPropertiesText(config);

    // Assert
    assertNotNull(props);
  }

  @Test
  @DisplayName("Should handle large configs")
  void testLargeConfigHandling() {
    // Arrange
    Map<String, Object> largeConfig = new LinkedHashMap<>();
    for (int i = 0; i < 1000; i++) {
      largeConfig.put("prop" + i, "value" + i);
    }

    // Act
    String props = configRepoService.toPropertiesText(largeConfig);

    // Assert
    assertNotNull(props);
    assertTrue(props.length() > 0);
  }

  @Test
  @DisplayName("Should return empty map for non-existent file")
  void testGetFileContents_NonExistent() {
    // Act
    Map<String, Object> contents = configRepoService.getFileContents("nonexistent_file_xyz.properties");

    // Assert
    assertNotNull(contents);
    assertTrue(contents.isEmpty());
  }

  @Test
  @DisplayName("Should return cached statistics structure")
  void testStatisticsStructure() {
    // Act
    Map<String, Object> stats = configRepoService.getCacheStatistics();

    // Assert
    assertTrue(stats.containsKey("cachedFilenames"));
    assertTrue(stats.containsKey("cachedConfigurations"));

    @SuppressWarnings("unchecked")
    List<String> filenames = (List<String>) stats.get("cachedFilenames");
    @SuppressWarnings("unchecked")
    List<String> configs = (List<String>) stats.get("cachedConfigurations");

    assertNotNull(filenames);
    assertNotNull(configs);
  }
}
