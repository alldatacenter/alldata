package com.linkedin.feathr.core.configbuilder.typesafe;

import com.linkedin.feathr.core.configdataprovider.ConfigDataProvider;
import com.linkedin.feathr.core.configdataprovider.UrlConfigDataProvider;
import com.linkedin.feathr.core.config.ConfigType;
import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import java.net.URL;
import java.util.Objects;


/**
 * Utility class to check if a config file is a Frame config file.
 */
public class FrameConfigFileChecker {
  private FrameConfigFileChecker() {
  }

  /**
   * Checks if a config file(file with conf extension) is a Frame config file or not.
   * A config file is a Frame feature config file if anchors, sources or derivations are present in the config
   * section. Metadata config files are not Frame feature config file.
   * A Frame config file can still contain invalid syntax. This is mainly used to collect all the Frame configs.
   */
  public static boolean isConfigFile(URL url) {
    try (ConfigDataProvider cdp = new UrlConfigDataProvider(url)) {
      Objects.requireNonNull(cdp, "ConfigDataProvider object can't be null");

      TypesafeConfigBuilder builder = new TypesafeConfigBuilder();

      Config config = builder.buildTypesafeConfig(ConfigType.FeatureDef, cdp);

      return config.hasPath(FeatureDefConfig.ANCHORS) || config.hasPath(FeatureDefConfig.DERIVATIONS) || config.hasPath(
          FeatureDefConfig.SOURCES);
    } catch (Exception e) {
      throw new ConfigBuilderException("Error in building config object", e);
    }
  }
}
