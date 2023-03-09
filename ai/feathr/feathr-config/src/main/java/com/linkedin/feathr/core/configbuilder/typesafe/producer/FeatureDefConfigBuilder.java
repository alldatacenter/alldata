package com.linkedin.feathr.core.configbuilder.typesafe.producer;

import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.config.producer.anchors.AnchorsConfig;
import com.linkedin.feathr.core.config.producer.derivations.DerivationsConfig;
import com.linkedin.feathr.core.config.producer.sources.SourcesConfig;
import com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors.AnchorsConfigBuilder;
import com.linkedin.feathr.core.configbuilder.typesafe.producer.derivations.DerivationsConfigBuilder;
import com.linkedin.feathr.core.configbuilder.typesafe.producer.sources.SourcesConfigBuilder;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.FeatureDefConfig.*;


/**
 * Builds the complete FeatureDefConfig object by delegating to its children, one per config section.
 */
public class FeatureDefConfigBuilder {
  private final static Logger logger = LogManager.getLogger(FeatureDefConfigBuilder.class);

  public static FeatureDefConfig build(Config config) {
    SourcesConfig sources = null;
    if (config.hasPath(SOURCES)) {
      Config sourcesCfg = config.getConfig(SOURCES);
      sources = SourcesConfigBuilder.build(sourcesCfg);
    }

    AnchorsConfig anchors = null;
    if (config.hasPath(ANCHORS)) {
      Config anchorsCfg = config.getConfig(ANCHORS);
      anchors = AnchorsConfigBuilder.build(anchorsCfg);
    }

    DerivationsConfig derivations = null;
    if (config.hasPath(DERIVATIONS)) {
      Config derivationCfg = config.getConfig(DERIVATIONS);
      derivations = DerivationsConfigBuilder.build(derivationCfg);
    }

    FeatureDefConfig configObj = new FeatureDefConfig(sources, anchors, derivations);
    //validateSemantics(configObj)        // TODO Semantic validation
    logger.debug("Built FeatureDefConfig object");

    return configObj;
  }

  /*
   * TODO: Semantic validation
   *  Validate:
   *  extractor class name refers to a valid class on the classpath
   *  source names, if any, in the anchors are resolved to those in the sources section
   *  date-time values are valid, i.e. not in the future and not too-far in the past
   */
  private Boolean validateSemantics(FeatureDefConfig configObj) {
    return true;
  }
}
