package com.linkedin.feathr.core.configvalidator;

/**
 * Runtime exception thrown if the config validation couldn't be performed. Any exceptions encountered during validation
 * itself will be provided in {@link ValidationResult}
 */
public class ConfigValidationException extends RuntimeException {
  public ConfigValidationException(String message) {
    super(message);
  }

  public ConfigValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
