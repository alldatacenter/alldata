package com.linkedin.feathr.common.types;

/**
 * A FeatureType class for Feathr's CATEGORICAL feature type.
 */
public class CategoricalFeatureType extends FeatureType {
  public static final CategoricalFeatureType INSTANCE = new CategoricalFeatureType();

  private CategoricalFeatureType() {
    super(BasicType.CATEGORICAL);
  }

}
