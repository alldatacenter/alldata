package com.linkedin.feathr.core.configbuilder.typesafe.consumer;

import com.linkedin.feathr.core.config.consumer.RelativeTimeRangeConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import java.time.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.consumer.RelativeTimeRangeConfig.*;


/**
 * Build the [[RelativeTimeRangeConfig]] class.
 * relativeTimeRange: {
 *     offset: 2d
 *     window: 3d
 *   }
 */
public class RelativeTimeRangeConfigBuilder {
  private final static Logger logger = LogManager.getLogger(RelativeTimeRangeConfigBuilder.class);

  private RelativeTimeRangeConfigBuilder() {
  }

  public static RelativeTimeRangeConfig build(Config relativeTimeRangeConfig) {
    Duration window = relativeTimeRangeConfig.hasPath(WINDOW) ? relativeTimeRangeConfig.getDuration(WINDOW) : null;

    if (window == null) {
      throw new ConfigBuilderException("window is a required parameter in relativeTimeRange config object");
    }

    Duration offset = relativeTimeRangeConfig.hasPath(OFFSET) ? relativeTimeRangeConfig.getDuration(OFFSET) : null;

    RelativeTimeRangeConfig configObj = new RelativeTimeRangeConfig(window, offset);

    logger.debug("Built AbsoluteTimeRangeConfig object");

    return configObj;
  }
}
