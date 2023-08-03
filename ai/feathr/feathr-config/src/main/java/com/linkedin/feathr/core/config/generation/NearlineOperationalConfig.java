package com.linkedin.feathr.core.config.generation;

import java.util.List;

/*
 * Nearline Operational config currently has all the fields as Operational config.
 *
 * In nearline, we dont have time based configs like timeSetting, retention, simlateTimeDelay, enableIncremental.
 * We only have name, outputProcessorsListConfig.
 */
public class NearlineOperationalConfig extends OperationalConfig {

  public NearlineOperationalConfig(List<OutputProcessorConfig> outputProcessorsListConfig, String name) {
    super(outputProcessorsListConfig, name);
  }
}
