package com.linkedin.feathr.common.types;

/**
 * A FeatureType class for Feathr's TERM_VECTOR feature type.
 */
public class TermVectorFeatureType extends FeatureType {
  public static final TermVectorFeatureType INSTANCE = new TermVectorFeatureType();

  private TermVectorFeatureType() {
    super(BasicType.TERM_VECTOR);
  }
}
