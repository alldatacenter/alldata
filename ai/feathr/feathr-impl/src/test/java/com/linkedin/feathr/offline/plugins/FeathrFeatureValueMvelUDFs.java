package com.linkedin.feathr.offline.plugins;

import com.linkedin.feathr.common.FeatureValue;

public class FeathrFeatureValueMvelUDFs {
  private FeathrFeatureValueMvelUDFs() { }

  public static FeatureValue inverse_ffv(FeatureValue input) {
    return FeatureValue.createNumeric(1f / input.getAsNumeric());
  }

  public static FeatureValue inverse_float(float input) {
    return FeatureValue.createNumeric(1f / input);
  }

  public static FeatureValue duplicate_string_ffv(FeatureValue input) {
    return FeatureValue.createCategorical(input.getAsCategorical() + input.getAsCategorical());
  }

  public static FeatureValue duplicate_string(String input) {
    return FeatureValue.createCategorical(input + input);
  }
}
