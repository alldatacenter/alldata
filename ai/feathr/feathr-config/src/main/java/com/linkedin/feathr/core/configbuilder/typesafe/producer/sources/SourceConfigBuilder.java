package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.feathr.core.config.producer.sources.HdfsConfig;
import com.linkedin.feathr.core.config.producer.sources.SourceConfig;
import com.linkedin.feathr.core.config.producer.sources.SourceType;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.sources.SourceConfig.*;


/**
 * Build {@link SourceConfig} object
 */
class SourceConfigBuilder {
  private final static Logger logger = LogManager.getLogger(SourceConfigBuilder.class);

  private SourceConfigBuilder() {
  }

  public static SourceConfig build(String sourceName, Config sourceConfig) {
    SourceConfig configObj;
    if (sourceConfig.hasPath(TYPE)) {
      String sourceTypeStr = sourceConfig.getString(TYPE);

      SourceType sourceType = SourceType.valueOf(sourceTypeStr);
      switch (sourceType) {
        case HDFS:
          configObj = HdfsConfigBuilder.build(sourceName, sourceConfig);
          break;

        case ESPRESSO:
          configObj = EspressoConfigBuilder.build(sourceName, sourceConfig);
          break;

        case RESTLI:
          configObj = RestliConfigBuilder.build(sourceName, sourceConfig);
          break;

        case VENICE:
          configObj = VeniceConfigBuilder.build(sourceName, sourceConfig);
          break;

        case KAFKA:
          configObj = KafkaConfigBuilder.build(sourceName, sourceConfig);
          break;

        case ROCKSDB:
          configObj = RocksDbConfigBuilder.build(sourceName, sourceConfig);
          break;

        case PASSTHROUGH:
          configObj = PassThroughConfigBuilder.build(sourceName, sourceConfig);
          break;

        case COUCHBASE:
          configObj = CouchbaseConfigBuilder.build(sourceName, sourceConfig);
          break;

        case CUSTOM:
          configObj = CustomSourceConfigBuilder.build(sourceName, sourceConfig);
          break;

        case PINOT:
          configObj = PinotConfigBuilder.build(sourceName, sourceConfig);
          break;

        default:
          throw new ConfigBuilderException("Unknown source type " + sourceTypeStr);
      }

    } else {
      // TODO: Remove. We'll make 'type' mandatory field.
      // default handling: it's assumed to be HDFS
      if (sourceConfig.hasPath(HdfsConfig.PATH)) {
        configObj = HdfsConfigBuilder.build(sourceName, sourceConfig);
      } else {
        throw new ConfigBuilderException("Unsupported source type for source " + sourceName);
      }
    }
    return configObj;
  }
}
