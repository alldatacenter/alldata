package com.linkedin.feathr.common.types;

/**
 * A FeatureType class for Feathr's CATEGORICAL_SET feature type.
 */
public class CategoricalSetFeatureType extends FeatureType {
  public static final CategoricalSetFeatureType INSTANCE = new CategoricalSetFeatureType();

  private CategoricalSetFeatureType() {
    super(BasicType.CATEGORICAL_SET);
  }

  // Someday: add a field for categoryType (or just dimensionType), and a constructor/factory, and extend the config
  // language to control this.
}
