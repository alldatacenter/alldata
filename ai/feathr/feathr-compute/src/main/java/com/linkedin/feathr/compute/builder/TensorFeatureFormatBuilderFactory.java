package com.linkedin.feathr.compute.builder;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.linkedin.feathr.core.config.producer.anchors.FeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.TimeWindowFeatureConfig;
import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import com.linkedin.feathr.core.config.producer.definitions.FeatureType;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfig;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;


/**
 * Factory class of {@link TensorTypeTensorFeatureFormatBuilder}. Given different feature type, it will return
 * different implementations or a empty builder.
 */
public class TensorFeatureFormatBuilderFactory {
  public TensorFeatureFormatBuilderFactory() {
  }

  /**
   * Get builder based on the featureType stored in the featureTypeConfig of the FeatureConfig, with one special case:
   * If feature type is not provided, but embedding size is set, we will build
   *   a {@link FeatureTypeTensorFeatureFormatBuilder} with feature type set as DENSE_VECTOR.
   * If feature type is not provided, and embedding size is not set, return empty build
   */
  public Optional<TensorFeatureFormatBuilder> getBuilder(@Nonnull FeatureConfig featureConfig) {
    Preconditions.checkNotNull(featureConfig);
    Optional<FeatureTypeConfig> featureTypeConfigOptional = featureConfig.getFeatureTypeConfig();

    // embeddingSize is set only when feature is a Sliding Window Aggregation feature, and that feature contains
    //  embeddingSize field
    Optional<Integer> embeddingSizeOptional = (featureConfig instanceof TimeWindowFeatureConfig)
        ? ((TimeWindowFeatureConfig) featureConfig).getEmbeddingSize() : Optional.empty();

    // Special case: if feature type is not provided
    if (!featureTypeConfigOptional.isPresent()) {
      // If embedding size is set in a Sliding Window Aggregation feature, we will build
      //   a {@link FeatureTypeTensorFeatureFormatBuilder} with feature type set as DENSE_VECTOR, since embedding implies it
      //   is a DENSE_VECTOR per Frame feature type.
      // Else build empty
      return embeddingSizeOptional.map(
          embeddingSize -> new FeatureTypeTensorFeatureFormatBuilder(FeatureType.DENSE_VECTOR, embeddingSize)
      );
    } else {
      return Optional.ofNullable(
          getBuilder(featureTypeConfigOptional.get(), embeddingSizeOptional.orElse(null), featureConfig.toString())
      );
    }
  }

  /**
   * Get builder based on the featureType stored in the featureTypeConfig of the derivationConfig
   */
  public Optional<TensorFeatureFormatBuilder> getBuilder(@Nonnull DerivationConfig derivationConfig) {
    Preconditions.checkNotNull(derivationConfig);
    return derivationConfig.getFeatureTypeConfig().map(
        featureTypeConfig -> getBuilder(featureTypeConfig, null, derivationConfig.toString())
    );
  }

  /**
   * Get builder based on the featureType stored in the featureTypeConfig:
   * 1. If the feature type is a legacy frame feature type, we will return
   *   a {@link FeatureTypeTensorFeatureFormatBuilder}, which maps frame feature type to Quince Tensor type and build
   *   {@link com.linkedin.feathr.compute.TensorFeatureFormat}.
   *
   * 2. If the feature type is a Quince Tensor type, we return {@link TensorTypeTensorFeatureFormatBuilder}.
   *
   * 3. If feature type is TENSOR, it means a FML feature, return empty build
   *
   * 4. If feature type is not supported, throw exception
   */
  private TensorFeatureFormatBuilder getBuilder(FeatureTypeConfig featureTypeConfig, Integer embeddingSize, String configRepresentation) {
    // embeddingSize can be null
    Preconditions.checkNotNull(featureTypeConfig);
    Preconditions.checkNotNull(configRepresentation);

    FeatureType featureType = featureTypeConfig.getFeatureType();
    if (FeatureTypeTensorFeatureFormatBuilder.VALID_FEATURE_TYPES.contains(featureType)) {
      return embeddingSize != null ? new FeatureTypeTensorFeatureFormatBuilder(featureType, embeddingSize)
          : new FeatureTypeTensorFeatureFormatBuilder(featureType);
    } else if (TensorTypeTensorFeatureFormatBuilder.VALID_FEATURE_TYPES.contains(featureType)) {
      return embeddingSize != null ? new TensorTypeTensorFeatureFormatBuilder(featureTypeConfig, embeddingSize)
          : new TensorTypeTensorFeatureFormatBuilder(featureTypeConfig);
    } else if (featureType == FeatureType.TENSOR) {
      return null;
    } else if (featureType == FeatureType.UNSPECIFIED) {
      throw new IllegalArgumentException("UNSPECIFIED feature type should not be used in config:" + configRepresentation);
    } else {
      Set<FeatureType> supportedFeatureTypes = Sets.union(
          FeatureTypeTensorFeatureFormatBuilder.VALID_FEATURE_TYPES,
          TensorTypeTensorFeatureFormatBuilder.VALID_FEATURE_TYPES);
      supportedFeatureTypes.add(FeatureType.TENSOR);
      throw new IllegalArgumentException(String.format("Feature type %s is not supported. The config is "
              + "is %s. Supported feature type are %s", featureType, configRepresentation,
          supportedFeatureTypes));
    }
  }
}
