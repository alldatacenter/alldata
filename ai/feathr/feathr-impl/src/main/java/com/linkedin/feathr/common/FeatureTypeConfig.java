package com.linkedin.feathr.common;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.linkedin.feathr.common.tensor.TensorType;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;


/**
 * This class encapsulates the type definition for a feature.  This implementation supports Feathr's high level semantic
 * type system (e.g. NUMERIC, BOOLEAN, VECTOR) and the new Tensor type system which includes additional metadata such
 * as category, dimensions, shape, value type and etc.
 *
 * When the feature type annotation from the feature definition is processed, the following scenarios are expected:
 * 1. No type annotated --> (FeatureType=UNSPECIFIED, TensorType=null)
 * 2. High level semantic NON-TENSOR feature types -->  (FeatureType=<some value>, TensorType=<mapped from feature type via auto-TZ>)
 * 3. Parameterized Tensor type annotated -->  (FeatureTypes = TENSOR, TensorType = <some value>)
 */
@JsonDeserialize(using = FeatureTypeConfigDeserializer.class)
public class FeatureTypeConfig implements Serializable {
  private final FeatureTypes _featureType;
  private final TensorType _tensorType;
  private final String _documentation;
  public static final FeatureTypeConfig UNDEFINED_TYPE_CONFIG = new FeatureTypeConfig(FeatureTypes.UNSPECIFIED);
  public static final FeatureTypeConfig NUMERIC_TYPE_CONFIG = new FeatureTypeConfig(FeatureTypes.NUMERIC);
  public static final FeatureTypeConfig DENSE_VECTOR_TYPE_CONFIG = new FeatureTypeConfig(FeatureTypes.DENSE_VECTOR);
  public static final FeatureTypeConfig TERM_VECTOR_TYPE_CONFIG = new FeatureTypeConfig(FeatureTypes.TERM_VECTOR);
  public static final FeatureTypeConfig CATEGORICAL_TYPE_CONFIG = new FeatureTypeConfig(FeatureTypes.CATEGORICAL);
  public static final FeatureTypeConfig CATEGORICAL_SET_TYPE_CONFIG = new FeatureTypeConfig(FeatureTypes.CATEGORICAL_SET);

  public FeatureTypeConfig() {
    this(FeatureTypes.UNSPECIFIED);
  }

  /**
   * Constructs a new instance with the legacy {@link FeatureTypes}. The corresponding {@link TensorType} will be populated

   */
  public FeatureTypeConfig(FeatureTypes featureType) {
    _featureType = featureType;
    if (featureType != FeatureTypes.UNSPECIFIED) {
      _tensorType = AutoTensorizableTypes.getDefaultTensorType(featureType).orElse(null);
    } else {
      _tensorType = null;
    }
    _documentation = null;
  }

  public String getDocumentation() {
    return _documentation;
  }
  /**
   * Constructs a new instance with the new {@link TensorType}.  The corresponding the legacy {@link FeatureTypes} will
   * be populated as {@code FeatureTypes.TENSOR}
   */
  public FeatureTypeConfig(@Nonnull TensorType tensorType) {
    Objects.requireNonNull(tensorType, "tensorType must be non-null when using the tensor type constructor");
    // Mark the legacy feature type to be TENSOR for to maintain semantic correctness where the presence of a TensorType
    // implies that the feature is a
    _featureType = FeatureTypes.TENSOR;
    _tensorType = tensorType;
    _documentation = null;
  }

  /**
   * Package private constructor where both the legacy type and tensor types are passed in. This constructor is reserved
   * for internal use only and does not provide any of consistency checking between legacy types and TensorType from the
   * other pubic constructors
   */
  public FeatureTypeConfig(@Nonnull FeatureTypes featureType, @Nonnull TensorType tensorType, String documentation) {
    Objects.requireNonNull(featureType, "featureType must be non-null");
    _featureType = featureType;
    _tensorType = tensorType;
    _documentation = documentation;
  }

  public boolean hasTensorType() {
    return _tensorType != null;
  }

  public FeatureTypes getFeatureType() {
    return _featureType;
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
    FeatureTypeConfig that = (FeatureTypeConfig) o;
    return _featureType == that._featureType && Objects.equals(_tensorType, that._tensorType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_featureType, _tensorType);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("FeatureTypeConfig{");
    sb.append("_featureType=").append(_featureType);
    sb.append(", _tensorType=").append(_tensorType);
    sb.append('}');
    return sb.toString();
  }
}
