package com.linkedin.feathr.core.configvalidator;

/**
 * Enum for config validation status.
 */
public enum ValidationStatus {
  VALID("valid"),
  WARN("warn"),         // Config is valid but has warnings
  INVALID("invalid"),
  PROCESSING_ERROR("processingError");     // error when processing Frame configs

  private final String _value;

  ValidationStatus(String value) {
    _value = value;
  }

  @Override
  public String toString() {
    return _value;
  }
}
