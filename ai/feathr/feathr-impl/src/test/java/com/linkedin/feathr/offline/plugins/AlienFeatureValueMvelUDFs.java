package com.linkedin.feathr.offline.plugins;

public class AlienFeatureValueMvelUDFs {
  private AlienFeatureValueMvelUDFs() { }

  public static AlienFeatureValue sqrt_afv(AlienFeatureValue input) {
    return AlienFeatureValue.fromFloat((float) Math.sqrt(input.getFloatValue()));
  }

  public static AlienFeatureValue sqrt_float(float input) {
    return AlienFeatureValue.fromFloat((float) Math.sqrt(input));
  }

  public static AlienFeatureValue uppercase_string_afv(AlienFeatureValue input) {
    return AlienFeatureValue.fromString(input.getStringValue().toUpperCase());
  }

  public static AlienFeatureValue uppercase_string(String input) {
    return AlienFeatureValue.fromString(input.toUpperCase());
  }

  public static AlienFeatureValue lowercase_string_afv(AlienFeatureValue input) {
    return AlienFeatureValue.fromString(input.getStringValue().toLowerCase());
  }

  public static AlienFeatureValue lowercase_string(String input) {
    return AlienFeatureValue.fromString(input.toLowerCase());
  }
}
