package com.linkedin.feathr.common.value;

import com.linkedin.feathr.common.types.CategoricalFeatureType;
import com.linkedin.feathr.common.types.FeatureType;
import java.util.Objects;


/**
 * A specific FeatureValue class for CATEGORICAL features. Underlying representation is a String.
 */
public class CategoricalFeatureValue implements FeatureValue {
  private static final FeatureType TYPE = CategoricalFeatureType.INSTANCE;
  private final String _stringValue;

  private CategoricalFeatureValue(String stringValue) {
    _stringValue = Objects.requireNonNull(stringValue);
  }

  /**
   * @param stringValue categorical string value
   * @return a CategoricalFeatureValue containing the provided string
   */
  public static CategoricalFeatureValue fromString(String stringValue) {
    return new CategoricalFeatureValue(stringValue);
  }

  /**
   * @return the contained categorical-string value
   */
  public String getStringValue() {
    return _stringValue;
  }

  @Override
  public FeatureType getFeatureType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CategoricalFeatureValue that = (CategoricalFeatureValue) o;
    return _stringValue.equals(that._stringValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_stringValue);
  }

  @Override
  public String toString() {
    return "CategoricalFeatureValue{" + "_stringValue='" + _stringValue + '\'' + '}';
  }
}
