package com.linkedin.feathr.compute.builder;


import com.google.common.base.Preconditions;
import com.linkedin.feathr.compute.FeatureValue;
import javax.annotation.Nonnull;


/**
 * Builder class that builds {@link FeatureValue} pegasus object that is used as the default value of a feature. This
 * default value will be used to populate feature data when missing data or error occurred while reading data.
 */
public class DefaultValueBuilder {
  private static final DefaultValueBuilder INSTANCE = new DefaultValueBuilder();
  public static DefaultValueBuilder getInstance() {
    return INSTANCE;
  }

  /**
   * Build default {@link FeatureValue}. Currently, only raw types, e.g., number, boolean, string, are supported.
   *
   */
  public FeatureValue build(@Nonnull Object featureValueObject) {
    Preconditions.checkNotNull(featureValueObject);
    FeatureValue featureValue = new FeatureValue();
    if (featureValueObject instanceof String) {
      featureValue.setString((String) featureValueObject);
    } else {
      throw new IllegalArgumentException(String.format("Default value %s has a unsupported type %s."
          + " Currently only support HOCON String."));
    }
    return featureValue;
  }
}
