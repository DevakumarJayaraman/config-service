package com.example.configservice.resolvers;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for secret resolvers.
 * Manages multiple secret resolver implementations and provides lookup by prefix.
 */
public class SecretResolverRegistry {

  private final Map<String, SecretResolver> resolvers;

  public SecretResolverRegistry() {
    this.resolvers = new HashMap<>();
    // Register built-in resolvers
    registerResolver(new VaultSecretResolver());
    registerResolver(new CyberArkSecretResolver());
  }

  /**
   * Registers a secret resolver.
   *
   * @param resolver the resolver to register
   */
  public void registerResolver(SecretResolver resolver) {
    resolvers.put(resolver.getPrefix(), resolver);
  }

  /**
   * Gets a resolver by its prefix.
   *
   * @param prefix the resolver prefix (e.g., "vault", "cyberark")
   * @return the resolver, or null if not found
   */
  public SecretResolver getResolver(String prefix) {
    return resolvers.get(prefix);
  }

  /**
   * Checks if a resolver exists for the given prefix.
   *
   * @param prefix the resolver prefix
   * @return true if a resolver exists
   */
  public boolean hasResolver(String prefix) {
    return resolvers.containsKey(prefix);
  }

  /**
   * Gets all registered resolver prefixes.
   *
   * @return set of registered prefixes
   */
  public java.util.Set<String> getRegisteredPrefixes() {
    return resolvers.keySet();
  }
}
