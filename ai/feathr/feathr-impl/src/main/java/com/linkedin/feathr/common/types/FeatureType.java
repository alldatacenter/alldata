package com.linkedin.feathr.common.types;

import com.linkedin.feathr.common.Experimental;
import com.linkedin.feathr.common.InternalApi;


/**
 * Top level interface for Feature Types in Feathr. Implementing classes should pertain to the various feature types
 * defined in Feathr's configuration language.
 */
public abstract class FeatureType {
  /**
   * Enum for the top-level feature types supported by Feathr.
   *
   * The enum values conform to specific subtypes of FeatureType and FeatureValue.
   * In the current API, if you see a FeatureType with .getBasicType() == NUMERIC, then you may assume the FeatureType
   * is a NumericFeatureType and any FeatureValue associated with it is a NumericFeatureValue. Etc.
   * This guarantee is mainly for convenience and clarity in the code within Feathr, and IT IS POSSIBLE THAT THIS
   * GUARANTEE MIGHT NOT HOLD TRUE in subsequent versions, so external code outside of Feathr should not rely on it.
   */
  @InternalApi
  public enum BasicType {
    BOOLEAN,
    NUMERIC,
    CATEGORICAL,
    CATEGORICAL_SET,
    TERM_VECTOR,
    DENSE_VECTOR,
    TENSOR
  }

  private final BasicType _basicType;

  protected FeatureType(BasicType basicType) {
    _basicType = basicType;
  }

  /**
   * See note on {@link BasicType}
   */
  @InternalApi
  public final BasicType getBasicType() {
    return _basicType;
  }
}
