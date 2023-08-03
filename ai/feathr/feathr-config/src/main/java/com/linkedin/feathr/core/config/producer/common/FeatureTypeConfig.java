package com.linkedin.feathr.core.config.producer.common;

import com.linkedin.feathr.core.config.ConfigObj;
import com.linkedin.feathr.core.config.producer.definitions.FeatureType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;


/**
 * Represents a type configuration for a feature by specifying the object part in the following fragment:
 * 1. For a simple feature type
 * <pre>
 * {@code
 *   <feature name>: {
 *     type: <feature type>
 *   }
 * }
 * </pre>
 * 2. For a complex feature type
 * <pre>
 * {@code
 *   <feature name>: {
 *     type: {
 *       type: <feature type>
 *       tensorCategory: <category of the tensor: DENSE/SPARSE/RAGGED>
 *       shape: <shape of the tensor type, only applicable to tensor>
 *       dimensionType: <dimension type of the tensor type, only applicable to tensor>
 *       valType: <Value type of the tensor type, only applicable to tensor>
 *     }
 *   }
 * }
 * </pre>
 */
public class FeatureTypeConfig implements ConfigObj {
  public static final String TYPE = "type";
  public static final String TENSOR_CATEGORY = "tensorCategory";
  public static final String SHAPE = "shape";
  public static final String DIMENSION_TYPE = "dimensionType";
  public static final String VAL_TYPE = "valType";
  private final FeatureType _featureType;
  private final Optional<List<Integer>> _shapes;
  private final Optional<List<String>> _dimensionTypes;
  private final Optional<String> _valType;

  private String _configStr;

  /**
   * Creates a FeatureTypeConfig.
   * @param shapes Shapes of the tensor(only applicable to tensor)
   * @param dimensionTypes Dimension types of the tensor(only applicable to tensor)
   * @param valType Value type of the tensor(only applicable to tensor)
   */
  private FeatureTypeConfig(@NonNull FeatureType featureType, List<Integer> shapes, List<String> dimensionTypes, String valType) {
    // Since VECTOR is deprecated, we always represent VECTOR with DENSE_VECTOR in Frame
    if (featureType == FeatureType.VECTOR) {
      _featureType = FeatureType.DENSE_VECTOR;
    } else {
      _featureType = featureType;
    }
    _shapes = Optional.ofNullable(shapes);
    _dimensionTypes = Optional.ofNullable(dimensionTypes);
    _valType = Optional.ofNullable(valType);

    constructConfigStr();
  }

  public FeatureTypeConfig(@NonNull  FeatureType featureType) {
    this(featureType, null, null, null);
  }

  public FeatureType getFeatureType() {
    return _featureType;
  }

  private void constructConfigStr() {
    StringBuilder sb = new StringBuilder();
    sb.append(FeatureTypeConfig.TYPE).append(": ").append(_featureType).append("\n");
    _shapes.ifPresent(t -> sb.append(FeatureTypeConfig.SHAPE).append(": ").append(t).append("\n"));
    _dimensionTypes.ifPresent(v -> sb.append(FeatureTypeConfig.DIMENSION_TYPE).append(": ").append(v).append("\n"));
    _valType.ifPresent(v -> sb.append(FeatureTypeConfig.VAL_TYPE).append(": ").append(v).append("\n"));
    _configStr = sb.toString();
  }

  /**
   * The shape (sometimes called the “size” or “dense shape”) of the tensor. Given as an array of integers. The first
   * element gives the size of the first dimension in the tensor, the second element gives the size of the second
   * dimension, and so on. The length of the tensorShape array is the number of dimensions in the tensor, also called
   * the tensor's rank. For scalar (rank-0) features, tensorShape should appear as an empty array.
   */
  public Optional<List<Integer>> getShapes() {
    return _shapes;
  }

  /**
   * Array of the types for each dimension. Allowable values are "int", "long", or "string". Length must be equal to
   * length of tensorShape.
   */
  public Optional<List<String>> getDimensionTypes() {
    return _dimensionTypes;
  }

  /**
   * The value type. Must be "int", "long", "float", "double", "boolean", or "string".
   */
  public Optional<String> getValType() {
    return _valType;
  }

  /**
   * The string of the serialized config object.
   */
  public String getConfigStr() {
    return _configStr;
  }

  @Override
  public String toString() {
    return _configStr;
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
    return _featureType == that._featureType && Objects.equals(_shapes, that._shapes) && Objects.equals(_dimensionTypes,
        that._dimensionTypes) && Objects.equals(_valType, that._valType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_featureType, _shapes, _dimensionTypes, _valType);
  }

  /**
   * The builder for {@link FeatureTypeConfig}
   */
  public static class Builder {
    private FeatureType _featureType;
    private List<Integer> _shapes;
    private List<String> _dimensionTypes;
    private String _valType;

    public Builder setFeatureType(FeatureType featureType) {
      _featureType = featureType;
      return this;
    }

    public Builder setShapes(List<Integer> shapes) {
      _shapes = shapes;
      return this;
    }

    public Builder setDimensionTypes(List<String> dimensionTypes) {
      _dimensionTypes = dimensionTypes;
      return this;
    }

    public Builder setValType(String valType) {
      _valType = valType;
      return this;
    }

    /**
     * Builds a new {@link FeatureTypeConfig} with existing parameters
     * @return {@link FeatureTypeConfig} object
     */
    public FeatureTypeConfig build() {
      return new FeatureTypeConfig(this._featureType, this._shapes, this._dimensionTypes, this._valType);
    }
  }
}
