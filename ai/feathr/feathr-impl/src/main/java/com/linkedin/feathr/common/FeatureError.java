package com.linkedin.feathr.common;

import com.google.common.base.Objects;
import java.util.StringJoiner;


/**
 * An error associated with a particular feature containing an error msg and the corresponding {@link FeatureErrorCode}
 */
public class FeatureError {

  private final FeatureErrorCode _errorCode;
  private final String _errorMsg;

  public FeatureError(FeatureErrorCode errorCode) {
    this(errorCode, "");
  }

  public FeatureError(FeatureErrorCode errorCode, String errorMsg) {
    _errorCode = errorCode;
    _errorMsg = errorMsg;
  }

  public FeatureError(FeatureErrorCode errorCode, Throwable t) {
    this(errorCode, t.getMessage());
  }

  public FeatureErrorCode getErrorCode() {
    return _errorCode;
  }

  public String getErrorMsg() {
    return _errorMsg;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeatureError that = (FeatureError) o;
    return getErrorCode() == that.getErrorCode() && Objects.equal(getErrorMsg(), that.getErrorMsg());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getErrorCode(), getErrorMsg());
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", FeatureError.class.getSimpleName() + "[", "]").add("_errorCode=" + _errorCode)
        .add("_errorMsg='" + _errorMsg + "'")
        .toString();
  }
}
