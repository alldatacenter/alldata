package com.linkedin.feathr.common.value;

import com.google.common.collect.ImmutableMap;
import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.types.BooleanFeatureType;
import com.linkedin.feathr.common.types.CategoricalSetFeatureType;
import com.linkedin.feathr.common.types.CategoricalFeatureType;
import com.linkedin.feathr.common.types.DenseVectorFeatureType;
import com.linkedin.feathr.common.types.NumericFeatureType;
import com.linkedin.feathr.common.types.TermVectorFeatureType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;


/**
 * Utility functions for constructing FeatureValue instances.
 *
 * User code that produces FeatureValues, such as Anchor extractors (in yet-to-come new Anchor extractor API),
 * should prefer to use these when creating FeatureValues, when possible.
 */
public class FeatureValues {
  private FeatureValues() {
  }

  /**
   * Returns a numeric feature value for a given float value
   */
  public static NumericFeatureValue numeric(float floatValue) {
    return NumericFeatureValue.fromFloat(floatValue);
  }

  /**
   * Returns a numeric feature value from a given tensor, which must be a numeric-scalar
   */
  public static NumericFeatureValue numeric(TensorData tensor) {
    return QuinceFeatureFormatMapper.INSTANCE.toNumericFeatureValue(NumericFeatureType.INSTANCE, tensor);
  }

  /**
   * Returns a boolean feature value for a given boolean primitive value.
   * (We would have named this function "boolean" but that is not allowed since it's a Java keyword.)
   */
  public static BooleanFeatureValue bool(boolean booleanValue) {
    return BooleanFeatureValue.fromBoolean(booleanValue);
  }

  /**
   * Returns a boolean feature value for a given tensor, which must be a boolean-scalar.
   * (We would have named this function "boolean" but that is not allowed since it's a Java keyword.)
   */
  public static BooleanFeatureValue bool(TensorData tensor) {
    return QuinceFeatureFormatMapper.INSTANCE.toBooleanFeatureValue(BooleanFeatureType.INSTANCE, tensor);
  }

  /**
   * Returns a categorical feature value for a given string categorical value.
   */
  public static CategoricalFeatureValue categorical(String stringValue) {
    return CategoricalFeatureValue.fromString(stringValue);
  }

  /**
   * Returns a categorical feature for a given tensor value, assuming it is shaped as expected for a categorical feature.
   */
  public static CategoricalFeatureValue categorical(TensorData tensor) {
    return QuinceFeatureFormatMapper.INSTANCE.toCategoricalFeatureValue(CategoricalFeatureType.INSTANCE, tensor);
  }

  /**
   * Returns a categorical set feature for a given collection of strings.
   */
  public static CategoricalSetFeatureValue categoricalSet(Collection<String> strings) {
    return CategoricalSetFeatureValue.fromStrings(strings);
  }

  /**
   * Returns a categorical set feature for a given set of strings.
   */
  public static CategoricalSetFeatureValue categoricalSet(Set<String> strings) {
    return CategoricalSetFeatureValue.fromStringSet(strings);
  }

  /**
   * Returns a categorical set feature for a given list of String arguments.
   */
  public static CategoricalSetFeatureValue categoricalSet(String... strings) {
    return categoricalSet(Arrays.asList(strings));
  }

  /**
   * Returns an empty categorical set.
   */
  public static CategoricalSetFeatureValue emptyCategoricalSet() {
    return categoricalSet(Collections.emptyList());
  }

  /**
   * Returns a categorical feature for a given tensor value, assuming it is shaped as expected for a categorical set feature.
   */
  public static CategoricalSetFeatureValue categoricalSet(TensorData tensorData) {
    return QuinceFeatureFormatMapper.INSTANCE.toCategoricalSetFeatureValue(CategoricalSetFeatureType.INSTANCE, tensorData);
  }

  /**
   * Returns a dense-vector feature value of unknown/dynamic size for a given list of float arguments.
   */
  public static DenseVectorFeatureValue denseVectorDynamicSize(float... floatArray) {
    return DenseVectorFeatureValue.fromFloatArray(floatArray);
  }

  /**
   * Returns a dense-vector feature value for a given tensor, which must be a rank-1 tensor of value-type float.
   */
  public static DenseVectorFeatureValue denseVector(TensorData tensorData) {
    return QuinceFeatureFormatMapper.INSTANCE.toDenseVectorFeatureValue(DenseVectorFeatureType.withUnknownSize(), tensorData);
  }

  /**
   * Returns a term-vector feature value for a given term-vector map.
   */
  public static TermVectorFeatureValue termVector(Map<String, Float> termVector) {
    return TermVectorFeatureValue.fromMap(termVector);
  }

  /**
   * Returns a term-vector feature value for a given tensor, which must be rank-1 and have numeric value type.
   */
  public static TermVectorFeatureValue termVector(TensorData tensorData) {
    return QuinceFeatureFormatMapper.INSTANCE.toTermVectorFeatureValue(TermVectorFeatureType.INSTANCE, tensorData);
  }

  /**
   * Returns a term-vector feature value with a single mapping, given by the term and value arguments.
   */
  public static TermVectorFeatureValue termVector(String t0, float v0) {
    return termVector(ImmutableMap.of(t0, v0));
  }

  /**
   * Returns a term-vector feature value with 2 mappings, given by the term and value arguments.
   */
  public static TermVectorFeatureValue termVector(String t0, float v0, String t1, float v1) {
    return termVector(ImmutableMap.of(t0, v0, t1, v1));
  }

  /**
   * Returns a term-vector feature value with 3 mappings, given by the term and value arguments.
   */
  public static TermVectorFeatureValue termVector(String t0, float v0, String t1, float v1, String t2, float v2) {
    return termVector(ImmutableMap.of(t0, v0, t1, v1, t2, v2));
  }

  /**
   * Returns an empty term-vector feature value.
   */
  public static TermVectorFeatureValue emptyTermVector() {
    return termVector(ImmutableMap.of());
  }
}
