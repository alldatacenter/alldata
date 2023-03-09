package com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors;

import com.linkedin.feathr.core.config.producer.anchors.AnchorConfig;
import com.linkedin.feathr.core.config.producer.anchors.AnchorsConfig;
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
 * Builds a map of anchor name to its config by delegating the building of each anchor config object
 * to its child
 */
public class AnchorsConfigBuilder {
  private final static Logger logger = LogManager.getLogger(AnchorsConfigBuilder.class);

  private AnchorsConfigBuilder() {
  }

  /**
   * config represents the object part in:
   * {@code anchors : { ... } }
   */
  public static AnchorsConfig build(Config config) {
    ConfigObject configObj = config.root();

    Stream<String> anchorNames = configObj.keySet().stream();

    Map<String, AnchorConfig> nameConfigMap = anchorNames.collect(
        Collectors.toMap(Function.identity(), aName -> AnchorConfigBuilder.build(aName, config.getConfig(quote(aName)))));

    AnchorsConfig anchorsConfig = new AnchorsConfig(nameConfigMap);
    logger.debug("Built all AnchorConfig objects");

    return anchorsConfig;
  }
}
