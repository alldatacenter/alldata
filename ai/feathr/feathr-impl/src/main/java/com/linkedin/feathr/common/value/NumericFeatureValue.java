package com.linkedin.feathr.common.value;

import com.linkedin.feathr.common.types.FeatureType;
import com.linkedin.feathr.common.types.NumericFeatureType;
import java.util.Objects;


/**
 * A specific FeatureValue class for NUMERIC features. Underlying representation is a float primitive value.
 */
public class NumericFeatureValue implements FeatureValue {
  private static final FeatureType TYPE = NumericFeatureType.INSTANCE;
  private final float _floatValue;

  private NumericFeatureValue(float floatValue) {
    _floatValue = floatValue;
  }

  /**
   * @param floatValue a float value
   * @return a numeric feature value for the given float value
   */
  public static NumericFeatureValue fromFloat(float floatValue) {
    return new NumericFeatureValue(floatValue);
  }

  /**
   * @return the contained float value
   */
  public float getFloatValue() {
    return _floatValue;
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
    NumericFeatureValue that = (NumericFeatureValue) o;
    return Float.compare(that._floatValue, _floatValue) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_floatValue);
  }

  @Override
  public String toString() {
    return "NumericFeatureValue{" + "_floatValue=" + _floatValue + '}';
  }
}
