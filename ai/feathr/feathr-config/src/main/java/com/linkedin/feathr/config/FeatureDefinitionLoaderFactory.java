package com.linkedin.feathr.config;

import com.linkedin.feathr.core.configbuilder.ConfigBuilder;


/**
 * Factory of {@link FeatureDefinitionLoader}
 */
public class FeatureDefinitionLoaderFactory {
  private static FeatureDefinitionLoader _instance;

  private FeatureDefinitionLoaderFactory() {
  }

  /**
   * Get an instance of {@link FeatureDefinitionLoader}.
   */
  public static FeatureDefinitionLoader getInstance() {
    if (_instance == null) {
      _instance = new FeatureDefinitionLoader(ConfigBuilder.get());
    }
    return _instance;
  }
}
