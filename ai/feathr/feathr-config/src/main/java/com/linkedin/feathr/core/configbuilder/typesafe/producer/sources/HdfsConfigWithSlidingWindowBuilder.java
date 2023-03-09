package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.feathr.core.config.producer.sources.HdfsConfigWithSlidingWindow;
import com.linkedin.feathr.core.config.producer.sources.SlidingWindowAggrConfig;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.sources.HdfsConfig.*;


/**
 * Build {@link HdfsConfigWithSlidingWindow} objects
 */
class HdfsConfigWithSlidingWindowBuilder {
  private final static Logger logger = LogManager.getLogger(HdfsConfigWithSlidingWindowBuilder.class);

  private HdfsConfigWithSlidingWindowBuilder() {
  }

  public static HdfsConfigWithSlidingWindow build(String sourceName, Config sourceConfig) {
    String path = sourceConfig.getString(PATH);
    String timePartitionPattern = sourceConfig.hasPath(TIME_PARTITION_PATTERN)
        ? sourceConfig.getString(TIME_PARTITION_PATTERN) : null;

    SlidingWindowAggrConfig swaConfigObj = SlidingWindowAggrConfigBuilder.build(sourceConfig);

    HdfsConfigWithSlidingWindow configObj = new HdfsConfigWithSlidingWindow(sourceName, path, timePartitionPattern, swaConfigObj);

    logger.trace("Built HdfsConfigWithSlidingWindow object for source " + sourceName);

    return configObj;
  }
}
