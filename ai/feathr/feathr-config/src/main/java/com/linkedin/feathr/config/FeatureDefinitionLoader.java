package com.linkedin.feathr.config;

import com.google.common.base.Preconditions;
import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilder;
import com.linkedin.feathr.core.configdataprovider.ConfigDataProvider;
import javax.annotation.Nonnull;


/**
 * Loader class for hich encloses all characteristics of a feature, such as source and
 * transformation.
 */
public class FeatureDefinitionLoader {
  private final ConfigBuilder _configBuilder;


  /**
   * Constructor.
   * @param configBuilder Interface for building {@link FeatureDefConfig} from a
   *                      HOCON-based Frame config.
   */
  public FeatureDefinitionLoader(@Nonnull ConfigBuilder configBuilder) {
    Preconditions.checkNotNull(configBuilder);
    _configBuilder = configBuilder;
  }

  public FeatureDefConfig loadAllFeatureDefinitions(@Nonnull ConfigDataProvider
      configDataProvider) {
    Preconditions.checkNotNull(configDataProvider);
    FeatureDefConfig featureDefConfig = _configBuilder.buildFeatureDefConfig(configDataProvider);

    return featureDefConfig;
  }
}
