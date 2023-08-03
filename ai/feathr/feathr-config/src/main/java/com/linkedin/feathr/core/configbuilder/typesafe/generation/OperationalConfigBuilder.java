package com.linkedin.feathr.core.configbuilder.typesafe.generation;

import com.linkedin.feathr.core.config.common.DateTimeConfig;
import com.linkedin.feathr.core.config.generation.NearlineOperationalConfig;
import com.linkedin.feathr.core.config.generation.OperationalConfig;
import com.linkedin.feathr.core.config.generation.OfflineOperationalConfig;
import com.linkedin.feathr.core.config.generation.OutputProcessorConfig;
import com.linkedin.feathr.core.utils.ConfigUtils;
import com.typesafe.config.Config;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Operation config object builder
 */

public class OperationalConfigBuilder {

  private final static Logger logger = LogManager.getLogger(OperationalConfigBuilder.class);
  private static final String NAME = "name";
  private static final String RETENTION = "retention";
  private static final String OUTPUT = "output";
  private static final String SIMULATE_TIME_DELAY = "timeDelay";
  private static final String ENABLE_INCREMENTAL = "enableIncremental";
  private static final String ENV = "env";

  private OperationalConfigBuilder() {
  }

  /**
   * Build operational config object in feature generation config file
   * default values: retention = 1 unit of time resolution, and simulate delay = 0
   */
  public static OperationalConfig build(Config config) {
    String name = config.getString(NAME);
    List<? extends Config> outputConfigs = config.getConfigList(OUTPUT);
    List<OutputProcessorConfig>
        outputProcessorConfigs = outputConfigs.stream().map(cfg -> OutputProcessorBuilder.build(cfg)).collect(Collectors.toList());
    OperationalConfig operationalConfig = null;

    // represents a nearline feature gen config, it should not have retention or any of the other time fields.
    if (config.hasPath(ENV) && config.getString(ENV).equals(OperationEnvironment.NEARLINE.toString())) {
      operationalConfig = new NearlineOperationalConfig(outputProcessorConfigs, name);
      logger.trace("Built OperationalConfig object for nearline feature");
    } else { // represents offline config. If env is not specified, it is offline by default. Env can be specified as offline also.
      // However, we do not need to check that case for now.
      DateTimeConfig dateTimeConfig = DateTimeConfigBuilder.build(config);
      Duration timeResolution = dateTimeConfig.get_timeResolution().getDuration();
      Duration retention = ConfigUtils.getDurationWithDefault(config, RETENTION, timeResolution);
      Duration simulateTimeDelay = ConfigUtils.getDurationWithDefault(config, SIMULATE_TIME_DELAY, Duration.ofSeconds(0));
      Boolean enableIncremental = ConfigUtils.getBooleanWithDefault(config, ENABLE_INCREMENTAL, false);

      operationalConfig =
          new OfflineOperationalConfig(outputProcessorConfigs, name, dateTimeConfig, retention, simulateTimeDelay,
              enableIncremental);
      logger.trace("Built OperationalConfig object for offline feature");
    }
    return operationalConfig;
  }
}
