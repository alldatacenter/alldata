package com.linkedin.feathr.core.configvalidator;

/**
 * Enum for the type of config validation to be performed
 */
public enum ValidationType {
  SYNTACTIC("syntactic"),
  SEMANTIC("semantic");

  private final String _value;

  ValidationType(String value) {
    _value = value;
  }

  @Override
  public String toString() {
    return _value;
  }
}
