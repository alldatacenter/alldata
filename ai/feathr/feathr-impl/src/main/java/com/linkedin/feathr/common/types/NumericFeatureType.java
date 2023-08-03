package com.linkedin.feathr.common.types;

/**
 * A FeatureType class for Feathr's NUMERIC feature type.
 */
public class NumericFeatureType extends FeatureType {
  public static final NumericFeatureType INSTANCE = new NumericFeatureType();

  private NumericFeatureType() {
    super(BasicType.NUMERIC);
  }

  // someday: add a field for valueType, and a constructor/factory, and extend the config language to control this.
}
