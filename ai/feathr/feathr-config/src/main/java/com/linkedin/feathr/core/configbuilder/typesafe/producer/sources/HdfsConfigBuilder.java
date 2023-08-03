package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.feathr.core.config.producer.sources.HdfsConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.sources.HdfsConfig.*;
import static com.linkedin.feathr.core.config.producer.sources.SlidingWindowAggrConfig.*;


/**
 * Builds HdfsConfig objects by delegating to child builders
 */
class HdfsConfigBuilder {
  private final static Logger logger = LogManager.getLogger(HdfsConfigBuilder.class);

  private HdfsConfigBuilder() {
  }

  public static HdfsConfig build(String sourceName, Config sourceConfig) {
    boolean hasTimePartitionPattern = sourceConfig.hasPath(TIME_PARTITION_PATTERN);
    boolean hasTimeSnapshot = sourceConfig.hasPath(HAS_TIME_SNAPSHOT);
    boolean hasIsTimeSeries = sourceConfig.hasPath(IS_TIME_SERIES);

    // hasTimeSnapshot and isTimeSeries were used to indicate a time-partitioned source.
    // isTimeSeries is used by sliding window aggregation and hasTimeSnapshot is used by time-aware join and time-based join.
    // In the unification effort(https://docs.google.com/document/d/1C6u2CKWSmOmHDQEL8Ovm5V5ZZFKhC_HdxVxU9D1F9lg/edit#),
    // they are replaced by the new field hasTimePartitionPattern. We only keep hasTimeSnapshot and isTimeSeries for backward-compatibility.
    // TODO - 12604) we should remove the legacy fields after the users migrate to new syntax
    if (hasTimePartitionPattern && (hasTimeSnapshot || hasIsTimeSeries)) {
      throw new ConfigBuilderException("hasTimeSnapshot and isTimeSeries are legacy fields. They cannot coexist with timePartitionPattern. "
          + "Please remove them from the source " + sourceName);
    }
    if (hasTimeSnapshot && hasIsTimeSeries) {
      throw new ConfigBuilderException("hasTimeSnapshot and isTimeSeries cannot coexist in source " + sourceName);
    }

    boolean hasSlidingWindowConfig = sourceConfig.hasPath(TIMEWINDOW_PARAMS);

    HdfsConfig configObj = hasSlidingWindowConfig ? HdfsConfigWithSlidingWindowBuilder.build(sourceName, sourceConfig)
        : HdfsConfigWithRegularDataBuilder.build(sourceName, sourceConfig);
    logger.debug("Built HdfsConfig object for source " + sourceName);

    return configObj;
  }
}
