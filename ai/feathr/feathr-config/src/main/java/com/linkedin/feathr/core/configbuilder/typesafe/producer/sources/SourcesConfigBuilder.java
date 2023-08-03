package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.feathr.core.config.producer.sources.SourceConfig;
import com.linkedin.feathr.core.config.producer.sources.SourcesConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.utils.Utils.*;


/**
 * Builds a map of source name to {@link SourceConfig} object. Each SourceConfig object is built by a child builder,
 * specific to the type of the source.
 */
public class SourcesConfigBuilder {
  private final static Logger logger = LogManager.getLogger(SourcesConfigBuilder.class);

  private SourcesConfigBuilder() {
  }

  /**
   * config represents the object part in:
   * {@code sources : { ... } }
   */
  public static SourcesConfig build(Config config) {
    ConfigObject configObj = config.root();
    Stream<String> sourceNames = configObj.keySet().stream();

    Map<String, SourceConfig> nameConfigMap = sourceNames.collect(
        Collectors.toMap(Function.identity(),
        sourceName -> SourceConfigBuilder.build(sourceName, config.getConfig(quote(sourceName))))
    );

    SourcesConfig sourcesConfig = new SourcesConfig(nameConfigMap);
    logger.debug("Built all SourceConfig objects");

    return sourcesConfig;
  }
}
