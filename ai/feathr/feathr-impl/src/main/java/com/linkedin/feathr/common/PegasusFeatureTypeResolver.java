package com.linkedin.feathr.common;

import com.google.common.annotations.VisibleForTesting;
import com.linkedin.feathr.compute.Dimension;
import com.linkedin.feathr.compute.FeatureVersion;
import com.linkedin.feathr.compute.TensorFeatureFormat;
import com.linkedin.feathr.common.tensor.DimensionType;
import com.linkedin.feathr.common.tensor.Primitive;
import com.linkedin.feathr.common.tensor.PrimitiveDimensionType;
import com.linkedin.feathr.common.types.PrimitiveType;
import com.linkedin.feathr.common.tensor.TensorCategory;
import com.linkedin.feathr.common.tensor.TensorType;
import com.linkedin.feathr.common.types.ValueType;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * This class maps from the pegasus models for feature types to Frame's common domain models for feature types and vice
 * versa.
 *
 * This creates a layer of indirection from the feature definition models expressed in Pegasus to the domain models used
 * by the frame's runtime engine (e.g. frame-online and frame-offline)
 *
 * @author bowu
 */
public class PegasusFeatureTypeResolver {

  private static final PegasusFeatureTypeResolver INSTANCE = new PegasusFeatureTypeResolver();

  public static PegasusFeatureTypeResolver getInstance() {
    return INSTANCE;
  }

  private PegasusFeatureTypeResolver() { }

  /**
   * Resolves the {@link FeatureTypeConfig} from the the pegasus {@link FeatureVersion} model.
   *
   * It's based on the following mapping rules:
   * - if `type` is TENSOR without `format` field, it is a FML tensor type
   * - if `type` is TENSOR with `format`, it is a Tensor feature type with FeatureTypeConfig in the feature definition
   * - if `type` is non-TENSOR without `format`, it is a legacy type
   * - if `type` is non-TENSOR with `format`, it is a legacy type with the format storing other info like embedding size
   *   that can be resolved using resolveEmbeddingSize(FeatureVersion)
   */
  public FeatureTypeConfig resolveFeatureType(FeatureVersion featureVersion) {
    FeatureTypes featureType = FeatureTypes.valueOf(featureVersion.getType().name());
    TensorType tensorType = null;

    // Even when featureType is not TENSOR, FeatureVersion still have format built
    if (featureType == FeatureTypes.TENSOR && featureVersion.hasFormat()) {
      tensorType = fromFeatureFormat(featureVersion.getFormat());
      // When the tensor format is present, then the frame feature type has to be TENSOR in case it is passed in
      // as the default value of UNSPECIFIED
      featureType = FeatureTypes.TENSOR;
    }

    // NOTE: it is possible to resolve the TensorType for FML tensor based features (FeatureTypes == TENSOR) here it is
    // purposely left out here to honor how {@link FeatureTypeConfig} should be handling FML tensor based features where
    // tensorType = null
    return tensorType != null ? new FeatureTypeConfig(featureType, tensorType, "No documentation") : new FeatureTypeConfig(featureType);
  }

  /**
   * Resolves the possible SWA embedding size from the pegasus {@link FeatureVersion} model.
   * The embedding size is valid only when the feature is a possible embedding feature (1-d vector), which means
   *  the feature type can only be DENSE_VECTOR, or TENSOR, or UNSPECIFIED. Meanwhile, the input FeatureVersion
   *  should have valid format information: 1) the format filed exists and is not null, 2) the shape size is 1.
   *
   * The API is scheduled to be deprecated after dropping legacy feature type support in Frame, after which the
   *  embedding size information will always be inside the {@link FeatureTypeConfig} built from {@link #resolveFeatureType}.
   *
   * Warning: this should be only used when you know the feature is an embedding feature.
   */
  @Deprecated
  public Optional<Integer> resolveEmbeddingSize(FeatureVersion featureVersion) {
    FeatureTypes featureType = FeatureTypes.valueOf(featureVersion.getType().name());
    // embedding size is meaningful only when the feature is embedding feature
    // embedding feature can only have type DENSE_VECTOR, or TENSOR, or UNSPECIFIED
    if (featureType != FeatureTypes.UNSPECIFIED && featureType != FeatureTypes.DENSE_VECTOR && featureType != FeatureTypes.TENSOR) {
      return Optional.empty();
    }
    // if FeatureVersion does not have format field, then there is no valid embedding size information
    if (!featureVersion.hasFormat()) {
      return Optional.empty();
    }

    TensorType tensorType = fromFeatureFormat(featureVersion.getFormat());
    int[] shape = tensorType.getShape();
    // if the shape length is not 1, the tensor type is not an equivalence of embedding (1-d vector)
    if (shape.length != 1) {
      return Optional.empty();
    }

    return Optional.of(shape[0]);
  }

  /**
   * Maps the {@link TensorFeatureFormat} pegasus model to the {@link TensorType} in quince.
   */
  private TensorType fromFeatureFormat(TensorFeatureFormat featureFormat) {
    ValueType valType = fromValueTypeEnum(featureFormat.getValueType());
    TensorCategory tensorCategory = TensorCategory.valueOf(featureFormat.getTensorCategory().name());
    List<DimensionType> dimensionTypes =
        featureFormat.getDimensions().stream().map(this::fromDimension).collect(Collectors.toList());
    // NOTE: TensorFeatureFormat does not model the dimensionNames so using null to trigger the default handling which
    // is to default to names taken from the dimensionTypes
    return new TensorType(tensorCategory, valType, dimensionTypes, null);
  }

  /**
   * Maps the {@link Dimension} in the pegasus model to the {@link DimensionType} from quince
   */
  @VisibleForTesting
  DimensionType fromDimension(Dimension pegasusDimension) {
    Integer shape = pegasusDimension.getShape();
    switch (pegasusDimension.getType()) {
      case LONG:
        return shape != null ? new PrimitiveDimensionType(Primitive.LONG, shape) : PrimitiveDimensionType.LONG;
      case INT:
        return shape != null ? new PrimitiveDimensionType(Primitive.INT, shape) : PrimitiveDimensionType.INT;
      case STRING:
        return shape != null ? new PrimitiveDimensionType(Primitive.STRING, shape) : PrimitiveDimensionType.STRING;
      // TODO: seems that Boolean primitive dimension types are not modeled in FR
      default:
        throw new IllegalArgumentException(
            "Unsupported dimension types from pegasus model: " + pegasusDimension.getType());
    }
  }

  /**
   * Maps the {@link com.linkedin.feathr.compute.ValueType} enum to the {@link ValueType} from quince
   *
   * Note: only primitives are supported at the moment
   */
  @VisibleForTesting
  ValueType fromValueTypeEnum(com.linkedin.feathr.compute.ValueType pegasusValType) {
    switch (pegasusValType) {
      case INT:
        return PrimitiveType.INT;
      case LONG:
        return PrimitiveType.LONG;
      case FLOAT:
        return PrimitiveType.FLOAT;
      case DOUBLE:
        return PrimitiveType.DOUBLE;
      case STRING:
        return PrimitiveType.STRING;
      case BOOLEAN:
        return PrimitiveType.BOOLEAN;
      default:
        throw new IllegalArgumentException("Unsupported value type from the pegasus model: " + pegasusValType);
    }
  }
}