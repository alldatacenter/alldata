package com.linkedin.feathr.common.types;

/**
 * A FeatureType class for Feathr's BOOLEAN feature type.
 */
public class BooleanFeatureType extends FeatureType {
  public static final BooleanFeatureType INSTANCE = new BooleanFeatureType();

  private BooleanFeatureType() {
    super(BasicType.BOOLEAN);
  }
}