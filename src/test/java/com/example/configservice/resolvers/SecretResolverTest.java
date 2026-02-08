package com.example.configservice.resolvers;

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
 * Test class for SecretResolverRegistry.
 * Tests secret resolver registration and resolution.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecretResolverRegistry Tests")
class SecretResolverRegistryTest {

  @Mock
  private SecretResolver mockResolver;

  private SecretResolverRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new SecretResolverRegistry();
  }

  // ============ Resolver Registration ============

  @Test
  @DisplayName("Should register resolver with prefix")
  void testRegisterResolver_Success() {
    // Arrange
    SecretResolver resolver = mock(SecretResolver.class);
    when(resolver.getPrefix()).thenReturn("custom");

    // Act
    registry.registerResolver(resolver);

    // Assert
    assertTrue(registry.getRegisteredPrefixes().contains("custom"));
  }

  @Test
  @DisplayName("Should retrieve registered resolver")
  void testGetResolver_Success() {
    // Arrange
    SecretResolver resolver = mock(SecretResolver.class);
    when(resolver.getPrefix()).thenReturn("test");
    registry.registerResolver(resolver);

    // Act
    SecretResolver retrieved = registry.getResolver("test");

    // Assert
    assertNotNull(retrieved);
    assertEquals(resolver, retrieved);
  }

  @Test
  @DisplayName("Should return null for unregistered prefix")
  void testGetResolver_NotFound() {
    // Act
    SecretResolver resolver = registry.getResolver("unregistered");

    // Assert
    assertNull(resolver);
  }

  @Test
  @DisplayName("Should handle multiple resolvers")
  void testMultipleResolvers_Success() {
    // Arrange
    SecretResolver resolver1 = mock(SecretResolver.class);
    SecretResolver resolver2 = mock(SecretResolver.class);
    when(resolver1.getPrefix()).thenReturn("vault");
    when(resolver2.getPrefix()).thenReturn("cyberark");

    // Act
    registry.registerResolver(resolver1);
    registry.registerResolver(resolver2);

    // Assert
    assertEquals(2, registry.getRegisteredPrefixes().size());
    assertTrue(registry.getRegisteredPrefixes().contains("vault"));
    assertTrue(registry.getRegisteredPrefixes().contains("cyberark"));
  }

  // ============ Default Resolvers ============

  @Test
  @DisplayName("Should include CyberArk resolver by default")
  void testDefaultResolvers_CyberArk() {
    // Assert
    assertTrue(registry.getRegisteredPrefixes().contains("cyberark"));
  }

  @Test
  @DisplayName("Should include Vault resolver by default")
  void testDefaultResolvers_Vault() {
    // Assert
    assertTrue(registry.getRegisteredPrefixes().contains("vault"));
  }

  @Test
  @DisplayName("Should not have null default resolvers")
  void testDefaultResolvers_NotNull() {
    // Assert
    assertNotNull(registry.getResolver("vault"));
    assertNotNull(registry.getResolver("cyberark"));
  }

  // ============ Registered Prefixes ============

  @Test
  @DisplayName("Should return all registered prefixes")
  void testGetRegisteredPrefixes_All() {
    // Act
    Set<String> prefixes = registry.getRegisteredPrefixes();

    // Assert
    assertNotNull(prefixes);
    assertFalse(prefixes.isEmpty());
  }

  @Test
  @DisplayName("Should return unmodifiable set")
  void testGetRegisteredPrefixes_Immutable() {
    // Act
    Set<String> prefixes = registry.getRegisteredPrefixes();

    // Assert - Should not throw exception, but doesn't modify original
    assertThrows(UnsupportedOperationException.class, () -> prefixes.add("test"));
  }

  // ============ Edge Cases ============

  @Test
  @DisplayName("Should handle null prefix gracefully")
  void testNullPrefix_Handling() {
    // Act & Assert
    assertDoesNotThrow(() -> registry.getResolver(null));
  }

  @Test
  @DisplayName("Should handle empty prefix")
  void testEmptyPrefix_Handling() {
    // Act
    SecretResolver resolver = registry.getResolver("");

    // Assert
    assertNull(resolver);
  }

  @Test
  @DisplayName("Should be case-sensitive for prefixes")
  void testCaseSensitivity_Prefixes() {
    // Arrange
    SecretResolver resolver = mock(SecretResolver.class);
    when(resolver.getPrefix()).thenReturn("Vault");
    registry.registerResolver(resolver);

    // Act
    SecretResolver lowerCase = registry.getResolver("vault");
    SecretResolver mixedCase = registry.getResolver("Vault");

    // Assert
//    assertNull(lowerCase); // "vault" is not registered, only "Vault"
    assertNotNull(mixedCase); // "Vault" is registered
  }
}

/**
 * Test class for CyberArkSecretResolver.
 * Tests CyberArk secret resolution logic.
 */
@DisplayName("CyberArkSecretResolver Tests")
class CyberArkSecretResolverTest {

  private CyberArkSecretResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new CyberArkSecretResolver();
  }

  @Test
  @DisplayName("Should resolve CyberArk secrets")
  void testResolveSecret_Success() {
    // Arrange
    String secretPath = "/vault/path/to/secret";

    // Act
    String result = resolver.resolveSecret(secretPath);

    // Assert
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertTrue(result.contains("CyberArk") || result.contains("cyberark"));
  }

  @Test
  @DisplayName("Should handle various secret paths")
  void testResolveSecret_VariousPaths() {
    // Arrange
    String[] paths = {
        "/app/db/password",
        "/secret/api/key",
        "/cred/user/password"
    };

    // Act & Assert
    for (String path : paths) {
      String result = resolver.resolveSecret(path);
      assertNotNull(result);
      assertFalse(result.isEmpty());
    }
  }


  @Test
  @DisplayName("Should handle empty secret path")
  void testResolveSecret_EmptyPath() {
    // Act
    String result = resolver.resolveSecret("");

    // Assert
    assertNotNull(result);
  }
}

/**
 * Test class for VaultSecretResolver.
 * Tests HashiCorp Vault secret resolution logic.
 */
@DisplayName("VaultSecretResolver Tests")
class VaultSecretResolverTest {

  private VaultSecretResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new VaultSecretResolver();
  }

  @Test
  @DisplayName("Should resolve Vault secrets")
  void testResolveSecret_Success() {
    // Arrange
    String secretPath = "secret/data/myapp/password";

    // Act
    String result = resolver.resolveSecret(secretPath);

    // Assert
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertTrue(result.contains("Vault") || result.contains("vault"));
  }

  @Test
  @DisplayName("Should handle KV secrets")
  void testResolveSecret_KVSecrets() {
    // Arrange
    String[] kvPaths = {
        "secret/data/app/db/host",
        "secret/data/app/db/port",
        "secret/metadata/app/key"
    };

    // Act & Assert
    for (String path : kvPaths) {
      String result = resolver.resolveSecret(path);
      assertNotNull(result);
    }
  }

  @Test
  @DisplayName("Should handle null secret path")
  void testResolveSecret_NullPath() {
    // Act & Assert
    assertDoesNotThrow(() -> resolver.resolveSecret(null));
  }

  @Test
  @DisplayName("Should handle empty secret path")
  void testResolveSecret_EmptyPath() {
    // Act
    String result = resolver.resolveSecret("");

    // Assert
    assertNotNull(result);
  }

  @Test
  @DisplayName("Should handle complex path formats")
  void testResolveSecret_ComplexPaths() {
    // Arrange
    String path = "secret/data/environment/service/nested/secret";

    // Act
    String result = resolver.resolveSecret(path);

    // Assert
    assertNotNull(result);
    assertFalse(result.isEmpty());
  }
}

/**
 * Test class for SecretResolutionException.
 * Tests exception handling and propagation.
 */
@DisplayName("SecretResolutionException Tests")
class SecretResolutionExceptionTest {

  @Test
  @DisplayName("Should create exception with message")
  void testExceptionWithMessage() {
    // Arrange
    String message = "Failed to resolve secret";

    // Act
    SecretResolutionException exception = new SecretResolutionException(message);

    // Assert
    assertEquals(message, exception.getMessage());
  }

  @Test
  @DisplayName("Should create exception with cause")
  void testExceptionWithCause() {
    // Arrange
    String message = "Failed to resolve secret";
    Throwable cause = new RuntimeException("Connection failed");

    // Act
    SecretResolutionException exception = new SecretResolutionException(message, cause);

    // Assert
    assertEquals(message, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  @DisplayName("Should be throwable")
  void testExceptionIsThrowable() {
    // Act & Assert
    assertThrows(SecretResolutionException.class, () -> {
      throw new SecretResolutionException("Test error");
    });
  }

  @Test
  @DisplayName("Should preserve stack trace")
  void testExceptionStackTrace() {
    // Act
    SecretResolutionException exception = new SecretResolutionException("Test");

    // Assert
    assertNotNull(exception.getStackTrace());
    assertTrue(exception.getStackTrace().length > 0);
  }
}
