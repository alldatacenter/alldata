package com.linkedin.feathr.common.value;

import com.google.common.primitives.Floats;
import com.linkedin.feathr.common.types.DenseVectorFeatureType;
import com.linkedin.feathr.common.types.FeatureType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;


/**
 * A specific FeatureValue class for DENSE_VECTOR features. Underlying representation is an array of float.
 */
public class DenseVectorFeatureValue implements FeatureValue {
  private static final FeatureType TYPE = DenseVectorFeatureType.withUnknownSize();
  private final float[] _floatArray;

  // someday: FeatureType will not be static, but will be passed in as a constructor arg, and will contain info about
  //          shape and valueType.

  private DenseVectorFeatureValue(float[] floatArray) {
    _floatArray = Objects.requireNonNull(floatArray);
  }

  private DenseVectorFeatureValue(Collection<? extends Number> numbers) {
    Objects.requireNonNull(numbers);
    _floatArray = Floats.toArray(numbers);
  }

  /**
   * @return a DenseVectorFeatureValue of unknown/dynamic size for the provided array of floats
   */
  public static DenseVectorFeatureValue fromFloatArray(float[] floatArray) {
    return new DenseVectorFeatureValue(floatArray);
  }

  /**
   * @return a DenseVectorFeatureValue of unknown/dynamic size for the provided collection of numbers
   */
  public static DenseVectorFeatureValue fromNumberList(Collection<? extends Number> numbers) {
    return new DenseVectorFeatureValue(numbers);
  }

  /**
   * @return the float array backing this dense-vector
   */
  public float[] getFloatArray() {
    return _floatArray;
  }

  @Override
  public FeatureType getFeatureType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DenseVectorFeatureValue that = (DenseVectorFeatureValue) o;
    return Arrays.equals(_floatArray, that._floatArray);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(_floatArray);
  }

  @Override
  public String toString() {
    return "DenseVectorFeatureValue{" + "_floatArray=" + Arrays.toString(_floatArray) + '}';
  }
}
