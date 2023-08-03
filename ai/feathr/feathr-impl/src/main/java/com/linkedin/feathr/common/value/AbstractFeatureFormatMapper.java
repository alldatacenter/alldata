package com.linkedin.feathr.common.value;

import com.linkedin.feathr.common.types.BooleanFeatureType;
import com.linkedin.feathr.common.types.CategoricalSetFeatureType;
import com.linkedin.feathr.common.types.CategoricalFeatureType;
import com.linkedin.feathr.common.types.DenseVectorFeatureType;
import com.linkedin.feathr.common.types.FeatureType;
import com.linkedin.feathr.common.types.NumericFeatureType;
import com.linkedin.feathr.common.types.TensorFeatureType;
import com.linkedin.feathr.common.types.TermVectorFeatureType;


/**
 * Abstract base class for FeatureFormatMapper. Breaks down conversion behavior based on the various feature types.
 */
public abstract class AbstractFeatureFormatMapper<T> implements FeatureFormatMapper<T> {
  @Override
  public T fromFeatureValue(FeatureValue featureValue) {
    switch (featureValue.getFeatureType().getBasicType()) {
      case BOOLEAN:
        return fromBooleanFeatureValue((BooleanFeatureValue) featureValue);
      case NUMERIC:
        return fromNumericFeatureValue((NumericFeatureValue) featureValue);
      case CATEGORICAL:
        return fromCategoricalFeatureValue((CategoricalFeatureValue) featureValue);
      case CATEGORICAL_SET:
        return fromCategoricalSetFeatureValue((CategoricalSetFeatureValue) featureValue);
      case TERM_VECTOR:
        return fromTermVectorFeatureValue((TermVectorFeatureValue) featureValue);
      case DENSE_VECTOR:
        return fromDenseVectorFeatureValue((DenseVectorFeatureValue) featureValue);
      case TENSOR:
        return fromTensorFeatureValue((TensorFeatureValue) featureValue);
      default:
        throw new IllegalStateException("Unexpected value: " + featureValue.getFeatureType().getBasicType());
    }
  }

  protected abstract T fromNumericFeatureValue(NumericFeatureValue featureValue);

  protected abstract T fromBooleanFeatureValue(BooleanFeatureValue featureValue);

  protected abstract T fromCategoricalFeatureValue(CategoricalFeatureValue featureValue);

  protected abstract T fromCategoricalSetFeatureValue(CategoricalSetFeatureValue featureValue);

  protected abstract T fromTermVectorFeatureValue(TermVectorFeatureValue featureValue);

  protected abstract T fromDenseVectorFeatureValue(DenseVectorFeatureValue featureValue);

  protected abstract T fromTensorFeatureValue(TensorFeatureValue featureValue);

  @Override
  public FeatureValue toFeatureValue(FeatureType featureType, T externalValue) {
    switch (featureType.getBasicType()) {
      case BOOLEAN:
        return toBooleanFeatureValue((BooleanFeatureType) featureType, externalValue);
      case NUMERIC:
        return toNumericFeatureValue((NumericFeatureType) featureType, externalValue);
      case CATEGORICAL:
        return toCategoricalFeatureValue((CategoricalFeatureType) featureType, externalValue);
      case CATEGORICAL_SET:
        return toCategoricalSetFeatureValue((CategoricalSetFeatureType) featureType, externalValue);
      case TERM_VECTOR:
        return toTermVectorFeatureValue((TermVectorFeatureType) featureType, externalValue);
      case DENSE_VECTOR:
        return toDenseVectorFeatureValue((DenseVectorFeatureType) featureType, externalValue);
      case TENSOR:
        return toTensorFeatureValue((TensorFeatureType) featureType, externalValue);
      default:
        throw new IllegalStateException("Unexpected value: " + featureType.getBasicType());
    }
  }

  protected abstract NumericFeatureValue toNumericFeatureValue(NumericFeatureType featureType, T externalValue);

  protected abstract BooleanFeatureValue toBooleanFeatureValue(BooleanFeatureType featureType, T externalValue);

  protected abstract CategoricalFeatureValue toCategoricalFeatureValue(CategoricalFeatureType featureType, T externalValue);

  protected abstract CategoricalSetFeatureValue toCategoricalSetFeatureValue(CategoricalSetFeatureType featureType, T externalValue);

  protected abstract TermVectorFeatureValue toTermVectorFeatureValue(TermVectorFeatureType featureType, T externalValue);

  protected abstract DenseVectorFeatureValue toDenseVectorFeatureValue(DenseVectorFeatureType featureType, T externalValue);

  protected abstract TensorFeatureValue toTensorFeatureValue(TensorFeatureType featureType, T externalValue);

  protected RuntimeException cannotConvertToFeatureValue(FeatureType featureType, T externalValue, Exception cause) {
    return new RuntimeException("Can't convert " + externalValue + " to feature value of type " + featureType, cause);
  }
  protected RuntimeException cannotConvertToFeatureValue(FeatureType featureType, T externalValue) {
    return cannotConvertToFeatureValue(featureType, externalValue, null);
  }
}
