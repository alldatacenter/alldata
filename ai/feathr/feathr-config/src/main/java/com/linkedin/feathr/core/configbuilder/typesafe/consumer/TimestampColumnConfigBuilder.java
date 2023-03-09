package com.linkedin.feathr.core.configbuilder.typesafe.consumer;

import com.linkedin.feathr.core.config.consumer.TimestampColumnConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.consumer.TimestampColumnConfig.*;

/**
 * Build the TimestampColumn config object.
 * timestampColumn: {
 *     def: timestamp
 *     format: yyyyMMdd
 *   }
 * @author rkashyap
 */
public class TimestampColumnConfigBuilder {
  private final static Logger logger = LogManager.getLogger(TimestampColumnConfigBuilder.class);

  private TimestampColumnConfigBuilder() {
  }

  public static TimestampColumnConfig build(Config timestampColumnConfig) {
    String name = timestampColumnConfig.hasPath(NAME) ? timestampColumnConfig.getString(NAME) : null;

    if (name == null) {
      throw new ConfigBuilderException(String.format("name is a required parameter in timestamp config object %s", timestampColumnConfig.toString()));
    }

    String format = timestampColumnConfig.hasPath(FORMAT) ? timestampColumnConfig.getString(FORMAT) : null;

    if (format == null) {
      throw new ConfigBuilderException(String.format("format is a required parameter in absoluteTimeRage config object %s", timestampColumnConfig.toString()));
    }

    TimestampColumnConfig configObj = new TimestampColumnConfig(name, format);

    logger.debug("Built Timestamp object");

    return configObj;
  }
}
