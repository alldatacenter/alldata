package com.linkedin.feathr.common.types;

import com.linkedin.feathr.common.tensor.TensorType;

import java.util.Objects;


/**
 * A FeatureType class for Feathr's TENSOR feature type.
 */
public class TensorFeatureType extends FeatureType {
  private final TensorType _tensorType;

  private TensorFeatureType(TensorType tensorType) {
    super(BasicType.TENSOR);
    _tensorType = Objects.requireNonNull(tensorType);
  }

  public static TensorFeatureType withTensorType(TensorType tensorType) {
    return new TensorFeatureType(tensorType);
  }

  public TensorType getTensorType() {
    return _tensorType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TensorFeatureType that = (TensorFeatureType) o;
    return _tensorType.equals(that._tensorType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_tensorType);
  }

  @Override
  public String toString() {
    return "TensorFeatureType{" + "_tensorType=" + _tensorType + '}';
  }
}
