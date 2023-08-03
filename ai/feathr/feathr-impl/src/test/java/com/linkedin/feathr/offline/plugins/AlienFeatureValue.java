package com.linkedin.feathr.offline.plugins;

import java.util.Objects;

public class AlienFeatureValue {
  private final Float floatValue;
  private final String stringValue;

  private AlienFeatureValue(Float floatValue, String stringValue) {
    this.floatValue = floatValue;
    this.stringValue = stringValue;
  }

  public static AlienFeatureValue fromFloat(float floatValue) {
    return new AlienFeatureValue(floatValue, null);
  }

  public static AlienFeatureValue fromString(String stringValue) {
    return new AlienFeatureValue(null, stringValue);
  }

  public boolean isFloat() {
    return floatValue != null;
  }

  public boolean isString() {
    return stringValue != null;
  }

  public float getFloatValue() {
    return Objects.requireNonNull(floatValue);
  }

  public String getStringValue() {
    return Objects.requireNonNull(stringValue);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AlienFeatureValue that = (AlienFeatureValue) o;
    return Objects.equals(floatValue, that.floatValue) && Objects.equals(stringValue, that.stringValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(floatValue, stringValue);
  }

  @Override
  public String toString() {
    return "AlienFeatureValue{" +
        "floatValue=" + floatValue +
        ", stringValue='" + stringValue + '\'' +
        '}';
  }
}
