package com.example.configservice.controllers;

import com.example.configservice.services.ComparisonService;
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
 * Test class for ComparisonController.
 * Tests configuration comparison endpoints.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ComparisonController Tests")
class ComparisonControllerTest {

  @Mock
  private ComparisonService comparisonService;

  private ComparisonController comparisonController;

  @BeforeEach
  void setUp() {
    comparisonController = new ComparisonController(comparisonService);
  }

  // ============ Compare Sources - Specific App ============

  @Test
  @DisplayName("Compare sources for app - should return differences")
  void testCompareSourcesForApp_Success() {
    // Arrange
    String app = "order-service";
    String profileA = "uat";
    String profileB = "prod";

    Map<String, Object> result = Map.of(
        "app", app,
        "profileA", profileA,
        "profileB", profileB,
        "differences", new ArrayList<>()
    );

    when(comparisonService.compareSourcesForApp(app, profileA, profileB)).thenReturn(result);

    // Act
    ResponseEntity<?> response = comparisonController.compareSourcesForApp(app, profileA, profileB);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    verify(comparisonService, times(1)).compareSourcesForApp(app, profileA, profileB);
  }

  @Test
  @DisplayName("Compare sources for app - should handle errors")
  void testCompareSourcesForApp_Error() {
    // Arrange
    String app = "service";
    String profileA = "a";
    String profileB = "b";

    when(comparisonService.compareSourcesForApp(app, profileA, profileB))
        .thenThrow(new RuntimeException("Test error"));

    // Act
    ResponseEntity<?> response = comparisonController.compareSourcesForApp(app, profileA, profileB);

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  // ============ Compare Sources - All Apps ============

  @Test
  @DisplayName("Compare sources for all apps - should return all comparisons")
  void testCompareSourcesForAllApps_Success() {
    // Arrange
    String profileA = "uat";
    String profileB = "prod";

    Map<String, Object> result = Map.of(
        "apps", "all",
        "profileA", profileA,
        "profileB", profileB,
        "appComparisons", new ArrayList<>()
    );

    when(comparisonService.compareSourcesForAllApps(profileA, profileB)).thenReturn(result);

    // Act
    ResponseEntity<?> response = comparisonController.compareSourcesForAllApps(profileA, profileB);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    verify(comparisonService, times(1)).compareSourcesForAllApps(profileA, profileB);
  }

  @Test
  @DisplayName("Compare sources for all apps - should handle errors")
  void testCompareSourcesForAllApps_Error() {
    // Arrange
    String profileA = "a";
    String profileB = "b";

    when(comparisonService.compareSourcesForAllApps(profileA, profileB))
        .thenThrow(new RuntimeException("Test error"));

    // Act
    ResponseEntity<?> response = comparisonController.compareSourcesForAllApps(profileA, profileB);

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  // ============ Compare Values - Specific App ============

  @Test
  @DisplayName("Compare values for app - should return value differences")
  void testCompareValuesForApp_Success() {
    // Arrange
    String app = "order-service";
    String profileA = "uat";
    String profileB = "prod";

    Map<String, Object> result = Map.of(
        "app", app,
        "profileA", profileA,
        "profileB", profileB,
        "differences", new ArrayList<>()
    );

    when(comparisonService.compareValuesForApp(app, profileA, profileB)).thenReturn(result);

    // Act
    ResponseEntity<?> response = comparisonController.compareValuesForApp(app, profileA, profileB);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    verify(comparisonService, times(1)).compareValuesForApp(app, profileA, profileB);
  }

  @Test
  @DisplayName("Compare values for app - should include comparison type")
  void testCompareValuesForApp_VerifyStructure() {
    // Arrange
    String app = "service";
    String profileA = "a";
    String profileB = "b";

    Map<String, Object> result = Map.of(
        "app", app,
        "comparisonType", "values",
        "differences", new ArrayList<>()
    );

    when(comparisonService.compareValuesForApp(app, profileA, profileB)).thenReturn(result);

    // Act
    ResponseEntity<?> response = comparisonController.compareValuesForApp(app, profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertEquals("values", body.get("comparisonType"));
  }

  @Test
  @DisplayName("Compare values for app - should handle errors")
  void testCompareValuesForApp_Error() {
    // Arrange
    String app = "service";
    String profileA = "a";
    String profileB = "b";

    when(comparisonService.compareValuesForApp(app, profileA, profileB))
        .thenThrow(new RuntimeException("Test error"));

    // Act
    ResponseEntity<?> response = comparisonController.compareValuesForApp(app, profileA, profileB);

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  // ============ Compare Values - All Apps ============

  @Test
  @DisplayName("Compare values for all apps - should return all app comparisons")
  void testCompareValuesForAllApps_Success() {
    // Arrange
    String profileA = "uat";
    String profileB = "prod";

    Map<String, Object> result = Map.of(
        "apps", "all",
        "profileA", profileA,
        "profileB", profileB,
        "appComparisons", new ArrayList<>()
    );

    when(comparisonService.compareValuesForAllApps(profileA, profileB)).thenReturn(result);

    // Act
    ResponseEntity<?> response = comparisonController.compareValuesForAllApps(profileA, profileB);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    verify(comparisonService, times(1)).compareValuesForAllApps(profileA, profileB);
  }

  @Test
  @DisplayName("Compare values for all apps - should handle errors")
  void testCompareValuesForAllApps_Error() {
    // Arrange
    String profileA = "a";
    String profileB = "b";

    when(comparisonService.compareValuesForAllApps(profileA, profileB))
        .thenThrow(new RuntimeException("Test error"));

    // Act
    ResponseEntity<?> response = comparisonController.compareValuesForAllApps(profileA, profileB);

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  // ============ Response Structure ============

  @Test
  @DisplayName("Should have consistent response structure")
  void testResponseStructure_Consistent() {
    // Arrange
    String app = "service";
    String profileA = "a";
    String profileB = "b";

    Map<String, Object> result = Map.of(
        "app", app,
        "profileA", profileA,
        "profileB", profileB,
        "comparisonType", "sources",
        "differences", new ArrayList<>(),
        "differenceCount", 0
    );

    when(comparisonService.compareSourcesForApp(app, profileA, profileB)).thenReturn(result);

    // Act
    ResponseEntity<?> response = comparisonController.compareSourcesForApp(app, profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertTrue(body.containsKey("app"));
    assertTrue(body.containsKey("profileA"));
    assertTrue(body.containsKey("profileB"));
    assertTrue(body.containsKey("differences"));
  }

  // ============ Error Handling ============

  @Test
  @DisplayName("Should return 500 on service exception")
  void testErrorHandling_ServiceException() {
    // Arrange
    String app = "service";
    String profileA = "a";
    String profileB = "b";

    when(comparisonService.compareSourcesForApp(app, profileA, profileB))
        .thenThrow(new RuntimeException("Unexpected error"));

    // Act
    ResponseEntity<?> response = comparisonController.compareSourcesForApp(app, profileA, profileB);

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertTrue(body.containsKey("error"));
  }

  @Test
  @DisplayName("Should preserve path variables in response")
  void testPathVariablesPreserved() {
    // Arrange
    String app = "my-app";
    String profileA = "staging";
    String profileB = "production";

    Map<String, Object> result = Map.of(
        "app", app,
        "profileA", profileA,
        "profileB", profileB,
        "differences", new ArrayList<>()
    );

    when(comparisonService.compareSourcesForApp(app, profileA, profileB)).thenReturn(result);

    // Act
    ResponseEntity<?> response = comparisonController.compareSourcesForApp(app, profileA, profileB);

    // Assert
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertEquals(app, body.get("app"));
    assertEquals(profileA, body.get("profileA"));
    assertEquals(profileB, body.get("profileB"));
  }
}
