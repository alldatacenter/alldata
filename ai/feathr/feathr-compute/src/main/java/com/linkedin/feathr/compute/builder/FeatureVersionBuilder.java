package com.linkedin.feathr.compute.builder;


import com.google.common.base.Preconditions;
import com.linkedin.feathr.compute.FeatureVersion;
import com.linkedin.feathr.core.config.producer.anchors.FeatureConfig;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfig;
import java.util.Optional;
import javax.annotation.Nonnull;


/**
 * Builder class that builds {@link FeatureVersion} pegasus object, which models a specific version of a feature. A
 * Feature can have multiple FeatureVersions. Versioning of a feature is declared by feature producers per semantic
 * versioning. Every time the definition of a feature changes, a new FeatureVersion should be created. Each
 * FeatureVersion enclosed attributes that don't change across environments.
 */
public class FeatureVersionBuilder {
  private final TensorFeatureFormatBuilderFactory _tensorFeatureFormatBuilderFactory;
  private final DefaultValueBuilder _defaultValueBuilder;
  private final FrameFeatureTypeBuilder _featureTypeBuilder;

  public FeatureVersionBuilder(@Nonnull TensorFeatureFormatBuilderFactory tensorFeatureFormatBuilderFactory,
      @Nonnull DefaultValueBuilder defaultValueBuilder, @Nonnull FrameFeatureTypeBuilder featureTypeBuilder) {
    Preconditions.checkNotNull(tensorFeatureFormatBuilderFactory);
    Preconditions.checkNotNull(defaultValueBuilder);
    Preconditions.checkNotNull(featureTypeBuilder);
    _tensorFeatureFormatBuilderFactory = tensorFeatureFormatBuilderFactory;
    _defaultValueBuilder = defaultValueBuilder;
    _featureTypeBuilder = featureTypeBuilder;
  }

  /**
   * Build {@link FeatureVersion} for anchored feature.
   */
  public FeatureVersion build(@Nonnull FeatureConfig featureConfig) {
    Preconditions.checkNotNull(featureConfig);
    FeatureVersion featureVersion = new FeatureVersion();
    Optional<TensorFeatureFormatBuilder> tensorFeatureFormatBuilder =
        _tensorFeatureFormatBuilderFactory.getBuilder(featureConfig);
    tensorFeatureFormatBuilder.ifPresent(builder ->
        featureVersion.setFormat(builder.build()));
    /*
     * Here if the FeatureTypeConfig contains a legacy feature type, set the type of FeatureVersion.
     * In downstream usage, if the `type` field exist, it will be used as the user defined feature type.
     * If the `type` field does not exist, we use the `format` field as the user defined tensor feature type.
     *
     * We still want to build the above `format` field even when the feature type is legacy type.
     * Because the `format` field contains other information such as embedding size for SWA feature.
     */
    featureConfig.getFeatureTypeConfig().flatMap(_featureTypeBuilder::build).ifPresent(featureVersion::setType);
    Optional<String> defaultValue = featureConfig.getDefaultValue();
    defaultValue.ifPresent(
        value -> featureVersion.setDefaultValue(_defaultValueBuilder.build(value))
    );
    return featureVersion;
  }

  /**
   * Build {@link FeatureVersion} for derived feature.
   */
  public FeatureVersion build(@Nonnull DerivationConfig derivationConfig) {
    Preconditions.checkNotNull(derivationConfig);

    FeatureVersion featureVersion = new FeatureVersion();
    Optional<TensorFeatureFormatBuilder> tensorFeatureFormatBuilder =
        _tensorFeatureFormatBuilderFactory.getBuilder(derivationConfig);
    tensorFeatureFormatBuilder.ifPresent(builder ->
        featureVersion.setFormat(builder.build()));
    /*
     * Here if the FeatureTypeConfig contains a legacy feature type, set the type of FeatureVersion.
     * In downstream usage, if the `type` field exist, it will be used as the user defined feature type.
     * If the `type` field does not exist, we use the `format` field as the user defined tensor feature type.
     *
     * We still want to build the above `format` field even when the feature type is legacy type.
     * Because the `format` field contains other information such as embedding size for SWA feature.
     */
    derivationConfig.getFeatureTypeConfig().flatMap(_featureTypeBuilder::build).ifPresent(featureVersion::setType);
    // TODO - add default value support for derived feature
    return featureVersion;
  }
}
