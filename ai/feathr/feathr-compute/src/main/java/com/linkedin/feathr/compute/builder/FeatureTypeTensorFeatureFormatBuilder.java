package com.linkedin.feathr.compute.builder;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.linkedin.feathr.compute.Dimension;
import com.linkedin.feathr.compute.DimensionArray;
import com.linkedin.feathr.compute.DimensionType;
import com.linkedin.feathr.compute.TensorCategory;
import com.linkedin.feathr.compute.ValueType;
import com.linkedin.feathr.core.config.producer.definitions.FeatureType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;


/**
 * Builder class for {@link com.linkedin.feathr.compute.TensorFeatureFormat} object given frame feature type.
 * In this case, the builder will map feature types to Quince tensor type. For example, frame feature type Numeric will
 * be mapped to Dense Tensor, with float value type and empty dimension. Detailed mapping rule is documented in:
 * https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/Frame+Auto-Tensorization+Type+Conversion+Rules
 */
class FeatureTypeTensorFeatureFormatBuilder extends TensorFeatureFormatBuilder {
  public static final Set<FeatureType> VALID_FEATURE_TYPES = Sets.immutableEnumSet(FeatureType.BOOLEAN,
      FeatureType.NUMERIC, FeatureType.CATEGORICAL, FeatureType.CATEGORICAL_SET, FeatureType.VECTOR,
      FeatureType.DENSE_VECTOR, FeatureType.TERM_VECTOR);
  private static final int UNKNOWN_DIMENSION_SIZE = -1;

  private FeatureType _featureType;
  private Optional<Integer> _embeddingSize;

  public FeatureTypeTensorFeatureFormatBuilder(@Nonnull FeatureType featureType) {
    super();
    Preconditions.checkNotNull(featureType);
    _featureType = featureType;
    _embeddingSize = Optional.empty();
  }

  /**
   * Constructor with embedding size. This should be used when feature has SlidingWindowEmbeddingAggregation
   * transformation function and embedding size is present.
   * @param featureType feature type.
   * @param embeddingSize embedding size.
   */
  public FeatureTypeTensorFeatureFormatBuilder(@Nonnull FeatureType featureType, int embeddingSize) {
    super();
    Preconditions.checkNotNull(featureType);
    _featureType = featureType;
    _embeddingSize = Optional.of(embeddingSize);
  }


  @Override
  void validCheck() {
    if (!VALID_FEATURE_TYPES.contains(_featureType)) {
      throw new IllegalArgumentException(String.format("Invalid feature type %s for TensorFeatureFormat. Valid types "
              + "are %s", _featureType, VALID_FEATURE_TYPES));
    }
    if (_embeddingSize.isPresent() && _featureType != FeatureType.DENSE_VECTOR) {
        throw new IllegalArgumentException(String.format("Dense vector feature type is expected when embedding size"
            + " is set. But provided type is %s", _featureType));
    }
  }

  @Override
  ValueType buildValueType() {
    return ValueType.FLOAT;
  }

  @Override
  DimensionArray buildDimensions() {
    List<Dimension> dimensions = new ArrayList<>();
    //For scalar, we set an empty dimension since dimension is pointless in this case.
    if (_featureType == FeatureType.NUMERIC || _featureType == FeatureType.BOOLEAN) {
      return new DimensionArray(dimensions);
    }
    Dimension dimension = new Dimension();
    if (_embeddingSize.isPresent()) {
      //Set embedding size as shape when present.
      dimension.setShape(_embeddingSize.get());
    } else {
      //For other feature types, we set dimension as -1, indicating the dimension is unknown.
      dimension.setShape(UNKNOWN_DIMENSION_SIZE);
    }
    switch (_featureType) {
      case CATEGORICAL:
      case CATEGORICAL_SET:
      case TERM_VECTOR:
        dimension.setType(DimensionType.STRING);
        break;
      case VECTOR:
      case DENSE_VECTOR:
        dimension.setType(DimensionType.INT);
        break;
      default:
        //This should not happen
        throw new IllegalArgumentException(String.format("Feature type %s is not supported. Valid types are: %s",
            _featureType, VALID_FEATURE_TYPES));
    }
    dimensions.add(dimension);
    return new DimensionArray(dimensions);
  }

  @Override
  TensorCategory buildTensorCategory() {
    switch (_featureType) {
      case BOOLEAN:
      case NUMERIC:
      case VECTOR:
      case DENSE_VECTOR:
        return TensorCategory.DENSE;
      case CATEGORICAL:
      case CATEGORICAL_SET:
      case TERM_VECTOR:
        return TensorCategory.SPARSE;
      default:
        throw new IllegalArgumentException(String.format("Feature type %s is not supported. Valid types are: %s",
            _featureType, VALID_FEATURE_TYPES));
    }
  }
}
