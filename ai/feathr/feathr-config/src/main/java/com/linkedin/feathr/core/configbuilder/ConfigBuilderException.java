package com.linkedin.feathr.core.configbuilder;

/**
 * When an error is encountered during config processing, this exception is thrown
 */
public class ConfigBuilderException extends RuntimeException {
  public ConfigBuilderException(String message) {
    super(message);
  }

  public ConfigBuilderException(String message, Throwable cause) {
    super(message, cause);
  }
}
