package com.linkedin.feathr.common.value;

import com.linkedin.feathr.common.GenericTypedTensor;
import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.TensorUtils;
import com.linkedin.feathr.common.TypedTensor;
import com.linkedin.feathr.common.types.FeatureType;
import com.linkedin.feathr.common.types.TensorFeatureType;
import java.util.Objects;


/**
 * A FeatureValue that contains an arbitrary tensor.
 */
public class TensorFeatureValue implements FeatureValue {
  private final TensorFeatureType _featureType;
  // Hold a reference to TypedTensor instead of TensorData for compatibility reasons. Some client code expects particular
  // subclasses of TypedTensor to be conveyed.
  private final TypedTensor _typedTensor;

  private TensorFeatureValue(TensorFeatureType featureType, TensorData tensorData) {
    _featureType = Objects.requireNonNull(featureType);
    _typedTensor = new GenericTypedTensor(Objects.requireNonNull(tensorData), _featureType.getTensorType());
  }

  private TensorFeatureValue(TypedTensor typedTensor) {
    Objects.requireNonNull(typedTensor);
    _featureType = TensorFeatureType.withTensorType(typedTensor.getType());
    _typedTensor = typedTensor;
  }

  /**
   * @return a tensor feature-value for the given tensor and feature-type
   */
  public static TensorFeatureValue fromTensorData(TensorFeatureType featureType, TensorData tensorData) {
    return new TensorFeatureValue(featureType, tensorData);
  }

  /**
   * @return a tensor feature-value for the given Quince TypedTensor
   */
  public static TensorFeatureValue fromTypedTensor(TypedTensor typedTensor) {
    return new TensorFeatureValue(typedTensor);
  }

  @Override
  public FeatureType getFeatureType() {
    return _featureType;
  }

  /**
   * @return the contained tensor as TensorData
   */
  public TensorData getAsTensor() {
    return _typedTensor.getData();
  }

  /**
   * @return the contained tensor as TypedTensor
   */
  public TypedTensor getAsTypedTensor() {
    return _typedTensor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TensorFeatureValue that = (TensorFeatureValue) o;
    return _featureType.equals(that._featureType) && _typedTensor.equals(that._typedTensor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_featureType, _typedTensor);
  }

  @Override
  public String toString() {
    String typedTensorDebugString = TensorUtils.getDebugString(_typedTensor.getType(), _typedTensor.getData(),
        TensorUtils.DEFAULT_MAX_STRING_LEN);
    return "TensorFeatureValue{" + "_featureType=" + _featureType + ", _typedTensor=" + typedTensorDebugString + '}';
  }
}
