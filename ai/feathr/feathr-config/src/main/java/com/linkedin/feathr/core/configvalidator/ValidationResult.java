package com.linkedin.feathr.core.configvalidator;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;


/**
 * Class to hold the configuration validation results
 */
public class ValidationResult {
  private ValidationType _type;
  private ValidationStatus _status;
  private String _details;
  private final Throwable _cause;

  // default valid results for different validation types
  public static final ValidationResult VALID_SYNTAX = new ValidationResult(ValidationType.SYNTACTIC, ValidationStatus.VALID);
  public static final ValidationResult VALID_SEMANTICS = new ValidationResult(ValidationType.SEMANTIC, ValidationStatus.VALID);

  public ValidationResult(ValidationType type, ValidationStatus status) {
    this(type, status, null, null);
  }

  public ValidationResult(ValidationType type, ValidationStatus status, String details) {
    this(type, status, details, null);
  }

  public ValidationResult(ValidationType type, ValidationStatus status, String details, Throwable cause) {
    Objects.requireNonNull(type, "ValidationType can't be null");
    Objects.requireNonNull(status, "ValidationStatus can't be null");

    _type = type;
    _status = status;
    _details = details;
    _cause = cause;
  }

  public ValidationType getValidationType() {
    return _type;
  }

  public ValidationStatus getValidationStatus() {
    return _status;
  }

  public Optional<String> getDetails() {
    return Optional.ofNullable(_details);
  }

  public Optional<Throwable> getCause() {
    return Optional.ofNullable(_cause);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ValidationResult result = (ValidationResult) o;
    return _type == result._type && _status == result._status && Objects.equals(_details, result._details)
        && Objects.equals(_cause, result._cause);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_type, _status, _details, _cause);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ValidationResult.class.getSimpleName() + "[", "]").add("type = " + _type)
        .add("status = " + _status)
        .add("details = '" + _details + "'")
        .add("cause = " + _cause)
        .toString();
  }
}
