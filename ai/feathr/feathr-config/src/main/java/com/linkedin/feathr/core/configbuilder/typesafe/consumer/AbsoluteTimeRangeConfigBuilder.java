package com.linkedin.feathr.core.configbuilder.typesafe.consumer;

import com.linkedin.feathr.core.config.consumer.AbsoluteTimeRangeConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.linkedin.feathr.core.utils.ConfigUtils;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.consumer.AbsoluteTimeRangeConfig.*;


/**
 * Build the [[AbsoluteTimeRangeConfig]] class object.
 * absoluteTimeRange: {
 *    startTime: 20200809
 *    endTime: 20200811
 *    timeFormat: yyyyMMdd
 *  }
 * @author rkashyap
 */
public class AbsoluteTimeRangeConfigBuilder {
  private final static Logger logger = LogManager.getLogger(AbsoluteTimeRangeConfigBuilder.class);

  private AbsoluteTimeRangeConfigBuilder() {
  }

  public static AbsoluteTimeRangeConfig build(Config absoluteTimeRangeConfig) {
    String startTime = absoluteTimeRangeConfig.hasPath(START_TIME) ? absoluteTimeRangeConfig.getString(START_TIME) : null;

    if (startTime == null) {
      throw new ConfigBuilderException(String.format("startTime is a required parameter in absoluteTimeRage config object %s", absoluteTimeRangeConfig));
    }

    String endTime = absoluteTimeRangeConfig.hasPath(END_TIME) ? absoluteTimeRangeConfig.getString(END_TIME) : null;

    if (endTime == null) {
      throw new ConfigBuilderException(String.format("endTime is a required parameter in absoluteTimeRage config object %s", absoluteTimeRangeConfig));
    }

    String timeFormat = absoluteTimeRangeConfig.hasPath(TIME_FORMAT) ? absoluteTimeRangeConfig.getString(TIME_FORMAT) : null;

    if (timeFormat == null) {
      throw new ConfigBuilderException(String.format("timeFormat is a required parameter in absoluteTimeRage config object %s", absoluteTimeRangeConfig));
    }

    // We only need to validate if the startTime/endTime corresponds to the given format, the actual conversion is done if frame offline.
    ConfigUtils.validateTimestampPatternWithEpoch(START_TIME, startTime, timeFormat);
    ConfigUtils.validateTimestampPatternWithEpoch(END_TIME, endTime, timeFormat);

    AbsoluteTimeRangeConfig configObj = new AbsoluteTimeRangeConfig(startTime, endTime, timeFormat);

    logger.debug("Built AbsoluteTimeRangeConfig object");

    return configObj;
  }
}
