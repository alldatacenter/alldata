package com.linkedin.feathr.core.configbuilder.typesafe.producer.derivations;

import com.linkedin.feathr.core.config.producer.derivations.DerivationConfig;
import com.linkedin.feathr.core.config.producer.derivations.DerivationsConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Builds a map of anchor name to its config by delegating the building of each anchor config object
 * to its child
 */
public class DerivationsConfigBuilder {
  private final static Logger logger = LogManager.getLogger(DerivationsConfigBuilder.class);

  private DerivationsConfigBuilder() {
  }

  /**
   * config represents the object part in:
   * {@code derivations : { ... }}
   */
  public static DerivationsConfig build(Config config) {
    logger.debug("Building DerivationConfig objects");
    ConfigObject configObj = config.root();

    Stream<String> derivedFeatureNames = configObj.keySet().stream();

    Map<String, DerivationConfig> nameConfigMap = derivedFeatureNames.collect(
        Collectors.toMap(Function.identity(),
        derivedFeatureName -> DerivationConfigBuilder.build(derivedFeatureName, config))
    );

    DerivationsConfig derivationsConfig = new DerivationsConfig(nameConfigMap);
    logger.debug("Built all DerivationConfig objects");

    return derivationsConfig;
  }
}
