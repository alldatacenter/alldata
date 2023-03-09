package com.linkedin.feathr.compute.builder;

import com.google.common.base.Preconditions;
import com.linkedin.feathr.compute.FrameFeatureType;
import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * Builder class that builds {@link FrameFeatureType} pegasus object that is used as the legacy type of a feature.
 */
public class FrameFeatureTypeBuilder {

  private static final FrameFeatureTypeBuilder INSTANCE = new FrameFeatureTypeBuilder();

  public static FrameFeatureTypeBuilder getInstance() {
    return INSTANCE;
  }

  private FrameFeatureTypeBuilder() {
    // singleton constructor
  }

  /**
   * Build {@link FrameFeatureType} pegasus object if [[FeatureTypeConfig]] contains legacy feature types
   */
  public Optional<FrameFeatureType> build(@Nonnull FeatureTypeConfig featureTypeConfig) {
    Preconditions.checkNotNull(featureTypeConfig);
    Preconditions.checkNotNull(featureTypeConfig.getFeatureType());

    FrameFeatureType featureType;

    if (featureTypeConfig.getFeatureType() == com.linkedin.feathr.core.config.producer.definitions.FeatureType.UNSPECIFIED) {
      throw new IllegalArgumentException("UNSPECIFIED feature type should not be used in feature config");
    } else if (TensorTypeTensorFeatureFormatBuilder.VALID_FEATURE_TYPES.contains(featureTypeConfig.getFeatureType())) {
      // high level type is always TENSOR, for DENSE_TENSOR, SPARSE_TENSOR, and RAGGED_TENSOR
      featureType = FrameFeatureType.TENSOR;
    } else {
      // For legacy type, since there is a 1:1 mapping of the types between com.linkedin.feathr.common.types.FeatureType
      //   and com.linkedin.feathr.core.config.producer.definitions.FeatureType for the rest types,
      //   build directly by name
      featureType = FrameFeatureType.valueOf(featureTypeConfig.getFeatureType().toString());
    }

    return Optional.of(featureType);
  }
}
