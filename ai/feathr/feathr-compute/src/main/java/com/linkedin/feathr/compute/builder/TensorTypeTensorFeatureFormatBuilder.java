package com.linkedin.feathr.compute.builder;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.linkedin.feathr.compute.Dimension;
import com.linkedin.feathr.compute.DimensionArray;
import com.linkedin.feathr.compute.DimensionType;
import com.linkedin.feathr.compute.TensorCategory;
import com.linkedin.feathr.compute.ValueType;
import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import com.linkedin.feathr.core.config.producer.definitions.FeatureType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;


/**
 * Builder class for {@link com.linkedin.feathr.compute.TensorFeatureFormat} object given
 * {@link FeatureTypeConfig}, when a Quince Tensor type is provided in the feature definition.
 */
public class TensorTypeTensorFeatureFormatBuilder extends TensorFeatureFormatBuilder {
  public static final Set<FeatureType> VALID_FEATURE_TYPES = Sets.immutableEnumSet(FeatureType.DENSE_TENSOR,
      FeatureType.SPARSE_TENSOR, FeatureType.RAGGED_TENSOR);

  private static final int UNKNOWN_DIMENSION_SIZE = -1;
  private FeatureTypeConfig _featureTypeConfig;
  private Optional<Integer> _embeddingSize;

  public TensorTypeTensorFeatureFormatBuilder(@Nonnull FeatureTypeConfig featureTypeConfig) {
    super();
    Preconditions.checkNotNull(featureTypeConfig);
    _featureTypeConfig = featureTypeConfig;
    _embeddingSize = Optional.empty();
  }

  /**
   * Constructor with embedding size. This should be used when feature has SlidingWindowEmbeddingAggregation
   * transformation function and embedding size is present.
   * @param featureTypeConfig feature type config.
   * @param embeddingSize embedding size.
   */
  public TensorTypeTensorFeatureFormatBuilder(@Nonnull FeatureTypeConfig featureTypeConfig, int embeddingSize) {
    super();
    Preconditions.checkNotNull(featureTypeConfig);
    _featureTypeConfig = featureTypeConfig;
    _embeddingSize = Optional.ofNullable(embeddingSize);
  }

  /**
   * Valid if provided {@link FeatureTypeConfig}. shapes and dimension types both need to present or not present at the
   * same time. If they both exist, they need to have the same size. The feature type need to be either Dense Tensor,
   * Sparse Tenser or Ragged Tensor. If embedding size is set, validate if an one-dimensional shape is provided and if
   * shape[0] matches embedding size.
   */
  @Override
  void validCheck() {
    if (!_featureTypeConfig.getDimensionTypes().isPresent() && _featureTypeConfig.getShapes().isPresent()) {
      throw new IllegalArgumentException(String.format("Shapes are provided but Dimensions are not provided in config"
          + "%s", _featureTypeConfig));
    }
    if (_featureTypeConfig.getDimensionTypes().isPresent() && _featureTypeConfig.getShapes().isPresent()
        && _featureTypeConfig.getDimensionTypes().get().size() != _featureTypeConfig.getShapes().get().size()) {
      throw new IllegalArgumentException(String.format("The size of dimension types %d and size of shapes %d are "
              + "unequal in config %s", _featureTypeConfig.getDimensionTypes().get().size(),
          _featureTypeConfig.getShapes().get().size(), _featureTypeConfig));
    }
    if (_featureTypeConfig.getShapes().isPresent()) {
      if (!_featureTypeConfig.getShapes().get()
          .stream().allMatch(shape -> shape > 0 || shape == UNKNOWN_DIMENSION_SIZE)) {
        throw new IllegalArgumentException(String.format("Shapes should be larger than 0 or -1. Provided shapes: %s",
            _featureTypeConfig.getShapes().get()));
      }
    }

    FeatureType featureType = _featureTypeConfig.getFeatureType();
    if (!VALID_FEATURE_TYPES.contains(featureType)) {
      throw new IllegalArgumentException(String.format("Invalid feature type %s for TensorFeatureFormat in config %s. "
          + "Valid types are %s", featureType, _featureTypeConfig, VALID_FEATURE_TYPES));
    }

    //Validate shapes when embedding size is set.
    if (_embeddingSize.isPresent()) {
        if (!_featureTypeConfig.getShapes().isPresent()) {
          throw new IllegalArgumentException(String.format("Shapes are not present while embedding size %d is set",
              _embeddingSize.get()));
        }
        if (_featureTypeConfig.getShapes().get().size() != 1) {
          throw new IllegalArgumentException(String.format("One dimensional shape is expected when embedding size"
              + " is set, but %s is provided", _featureTypeConfig.getShapes().get()));
        }
        if (!_featureTypeConfig.getShapes().get().get(0).equals(_embeddingSize.get())) {
          throw new IllegalArgumentException(String.format("Embedding size %s and shape size %s don't match",
              _embeddingSize.get(), _featureTypeConfig.getShapes().get().get(0)));
        }
        if (_featureTypeConfig.getFeatureType() != FeatureType.DENSE_TENSOR) {
          throw new IllegalArgumentException(String.format("Dense tensor feature type is expected when embedding size"
              + " is set. But provided type is %s", _featureTypeConfig.getFeatureType()));
        }
    }
  }

  @Override
  ValueType buildValueType() {
    if (!_featureTypeConfig.getValType().isPresent()) {
      throw new IllegalArgumentException(String.format("Value type is not specified in feature type config %s. "
              + "This is required to build TensorFeatureFormat", _featureTypeConfig));
    }
    return ValueType.valueOf(_featureTypeConfig.getValType().get().toUpperCase());
  }

  @Override
  DimensionArray buildDimensions() {
    List<Dimension> dimensions = new ArrayList<>();
    if (_featureTypeConfig.getDimensionTypes().isPresent()) {
      for (int i = 0; i < _featureTypeConfig.getDimensionTypes().get().size(); i++) {
        Dimension dimension = new Dimension();
        //TODO - 11753) set shapes when emebedding size of lateral view is present
        if (_featureTypeConfig.getShapes().isPresent()) {
          dimension.setShape(_featureTypeConfig.getShapes().get().get(i));
        } else {
          dimension.setShape(UNKNOWN_DIMENSION_SIZE);
        }
        DimensionType dimensionType = DimensionType.valueOf(
            _featureTypeConfig.getDimensionTypes().get().get(i).toUpperCase());
        dimension.setType(dimensionType);
        dimensions.add(dimension);
      }
    }
    return new DimensionArray(dimensions);
  }

  @Override
  TensorCategory buildTensorCategory() {
    FeatureType featureType = _featureTypeConfig.getFeatureType();
    switch (featureType) {
      case DENSE_TENSOR:
        return TensorCategory.DENSE;
      case SPARSE_TENSOR:
        return TensorCategory.SPARSE;
      case RAGGED_TENSOR:
        return TensorCategory.RAGGED;
      default:
        throw new IllegalArgumentException(String.format("Invalid feature type %s. Valid types are %s",
            featureType, VALID_FEATURE_TYPES));
    }
  }
}
