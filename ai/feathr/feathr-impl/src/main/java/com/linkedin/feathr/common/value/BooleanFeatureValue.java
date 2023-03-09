package com.linkedin.feathr.common.value;

import com.linkedin.feathr.common.types.BooleanFeatureType;
import com.linkedin.feathr.common.types.FeatureType;
import java.util.Objects;


/**
 * A specific FeatureValue class for BOOLEAN features. Underlying representation is a boolean primitive type.
 */
public class BooleanFeatureValue implements FeatureValue {
  public static final BooleanFeatureValue FALSE = new BooleanFeatureValue(false);
  public static final BooleanFeatureValue TRUE = new BooleanFeatureValue(true);

  private static final FeatureType TYPE = BooleanFeatureType.INSTANCE;

  private final boolean _booleanValue;

  private BooleanFeatureValue(boolean booleanValue) {
    _booleanValue = booleanValue;
  }

  /**
   * @param value a boolean value
   * @return a BooleanFeatureValue for the provided boolean
   */
  public static BooleanFeatureValue fromBoolean(boolean value) {
    return value ? TRUE : FALSE;
  }

  /**
   * @return the contained boolean value
   */
  public boolean getBooleanValue() {
    return _booleanValue;
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
    BooleanFeatureValue that = (BooleanFeatureValue) o;
    return _booleanValue == that._booleanValue;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_booleanValue);
  }

  @Override
  public String toString() {
    return "BooleanFeatureValue{" + "_booleanValue=" + _booleanValue + '}';
  }
}
