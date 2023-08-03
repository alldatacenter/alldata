package com.linkedin.feathr.common.types;

import java.util.Objects;


/**
 * A FeatureType class for Feathr's DENSE_VECTOR feature type.
 */
public class DenseVectorFeatureType extends FeatureType {
  public static final int UNKNOWN_SIZE = -1;
  private static final DenseVectorFeatureType UNKNOWN_SIZE_INSTANCE = new DenseVectorFeatureType(UNKNOWN_SIZE);

  private final int _size;

  private DenseVectorFeatureType(int size) {
    super(BasicType.DENSE_VECTOR);
    _size = size;
  }

  /**
   * Returns a DenseVectorType for a vector of known fixed size.
   * @param size The fixed vector size for features of this type. If a feature's type is DenseVectorFeatureType with
   *             size 100, then for all instances of this feature (in all examples/observations) the feature value
   *             will always be represented as an array of length 100.
   */
  public static DenseVectorFeatureType withSize(int size) {
    return (size == UNKNOWN_SIZE) ? UNKNOWN_SIZE_INSTANCE : new DenseVectorFeatureType(size);
  }

  /**
   * Returns a DenseVectorType for a vector of unknown/dynamic size. If a feature's type is DenseVectorFeatureType with
   * size UNKNOWN_SIZE, then instances of this feature in various examples/observations may have different sizes.
   */
  public static DenseVectorFeatureType withUnknownSize() {
    return UNKNOWN_SIZE_INSTANCE;
  }

  /**
   * @return the size, which might be {@link #UNKNOWN_SIZE}
   */
  public int getSize() {
    return _size;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DenseVectorFeatureType)) {
      return false;
    }
    DenseVectorFeatureType that = (DenseVectorFeatureType) o;
    return _size == that._size;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_size);
  }

  @Override
  public String toString() {
    return "DenseVectorType{" + "_size=" + _size + '}';
  }
}