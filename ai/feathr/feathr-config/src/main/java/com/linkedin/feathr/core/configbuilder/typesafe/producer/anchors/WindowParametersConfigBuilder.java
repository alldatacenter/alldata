package com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors;

import com.linkedin.feathr.core.config.WindowType;
import com.linkedin.feathr.core.config.producer.anchors.WindowParametersConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import java.time.Duration;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.anchors.FeatureConfig.*;

/**
 * Build {@link WindowParametersConfig} object
 */
public class WindowParametersConfigBuilder {
  private final static Logger logger = LogManager.getLogger(FeatureConfigBuilder.class);

  /*
   * Prevent instantiation of class from outside
   */
  private WindowParametersConfigBuilder() {
  }

  /*
   * Build a [[WindowParametersConfig]] object.
   * @param windowParametersConfig Config of windowParameters object mentioned in a feature.
   * @return WindowParametersConfig object
   */
  public static WindowParametersConfig build(Config windowParametersConfig) {
    String type = windowParametersConfig.getString(TYPE);
    WindowType windowType;
    try {
      windowType = WindowType.valueOf(type);
    } catch (IllegalArgumentException e) {
      throw new ConfigBuilderException("Unsupported window type " + type + "; expected one of "
          + Arrays.toString(WindowType.values()));
    }

    Duration size = windowParametersConfig.getDuration(SIZE);

    Duration slidingInterval = null;
    if (windowParametersConfig.hasPath(SLIDING_INTERVAL)) {
      slidingInterval = windowParametersConfig.getDuration(SLIDING_INTERVAL);
    }

    WindowParametersConfig configObj = new WindowParametersConfig(windowType, size, slidingInterval);

    return configObj;
  }
}
