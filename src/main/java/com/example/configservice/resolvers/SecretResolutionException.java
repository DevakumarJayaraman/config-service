package com.example.configservice.resolvers;

/**
 * Exception thrown when secret resolution fails.
 */
public class SecretResolutionException extends RuntimeException {

  public SecretResolutionException(String message) {
    super(message);
  }

  public SecretResolutionException(String message, Throwable cause) {
    super(message, cause);
  }
}
