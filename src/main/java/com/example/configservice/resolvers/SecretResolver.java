package com.example.configservice.resolvers;

/**
 * Interface for secret resolution from various secret management systems.
 */
public interface SecretResolver {
  /**
   * Resolves a secret from the configured secret management system.
   *
   * @param secretPath the path to the secret
   * @return the resolved secret value
   */
  String resolveSecret(String secretPath);

  /**
   * Gets the prefix/identifier for this resolver (e.g., "vault", "cyberark").
   *
   * @return the resolver prefix
   */
  String getPrefix();
}
