package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.feathr.core.config.producer.sources.HdfsConfigWithRegularData;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueType;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.sources.HdfsConfig.*;


/**
 * Builds HdfsConfigWithRegularData objects.
 */
class HdfsConfigWithRegularDataBuilder {
  private final static Logger logger = LogManager.getLogger(HdfsConfigWithRegularDataBuilder.class);

  private HdfsConfigWithRegularDataBuilder() {
  }

  public static HdfsConfigWithRegularData build(String sourceName, Config sourceConfig) {

    String path = sourceConfig.getString(PATH);
    String timePartitionPattern = sourceConfig.hasPath(TIME_PARTITION_PATTERN)
        ? sourceConfig.getString(TIME_PARTITION_PATTERN) : null;
    boolean hasTimeSnapshot = sourceConfig.hasPath(HAS_TIME_SNAPSHOT) && sourceConfig.getBoolean(HAS_TIME_SNAPSHOT);

    HdfsConfigWithRegularData configObj = new HdfsConfigWithRegularData(sourceName, path, timePartitionPattern, hasTimeSnapshot);
    logger.trace("Built HdfsConfigWithRegularData object for source" + sourceName);

    return configObj;
  }

  private static List<String> getStringList(Config sourceConfig, String field) {
    ConfigValueType valueType = sourceConfig.getValue(field).valueType();
    List<String> stringList;
    switch (valueType) {
      case STRING:
        stringList = Collections.singletonList(sourceConfig.getString(field));
        break;

      case LIST:
        stringList = sourceConfig.getStringList(field);
        break;

      default:
        throw new ConfigBuilderException("Expected " + field + " value type String or List, got " + valueType);
    }
    return stringList;
  };
}
