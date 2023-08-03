package com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors;

import com.linkedin.feathr.core.config.producer.anchors.AnchorConfigWithExtractor;
import com.linkedin.feathr.core.config.producer.anchors.FeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.TypedKey;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.linkedin.feathr.core.utils.ConfigUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueType;
import java.util.List;
import java.util.Map;
import javax.lang.model.SourceVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.anchors.AnchorConfig.*;


/**
 * Builds AnchorConfig objects that have features that are extracted via a udf class (an extractor)
 */
class AnchorConfigWithExtractorBuilder extends BaseAnchorConfigBuilder {
  private final static Logger logger = LogManager.getLogger(AnchorConfigWithExtractorBuilder.class);

  private AnchorConfigWithExtractorBuilder() {
  }

  public static AnchorConfigWithExtractor build(String name, Config config) {
    String source = config.getString(SOURCE);

    String extractor;
    String extractorClassName = config.hasPath(EXTRACTOR)
        ? getExtractorClassName(config)
        : getTransformerClassName(config);
    if (SourceVersion.isName(extractorClassName)) {
      extractor = extractorClassName;
    } else {
      throw new ConfigBuilderException("Invalid class name for extractor: " + extractorClassName);
    }

    String keyExtractor = config.hasPath(KEY_EXTRACTOR) ? config.getString(KEY_EXTRACTOR) : null;

    TypedKey typedKey = TypedKeyBuilder.getInstance().build(config);

    List<String> keyAlias = ConfigUtils.getStringList(config, KEY_ALIAS);

    if ((keyAlias != null || typedKey != null) && keyExtractor != null) {
      throw new ConfigBuilderException("The keyExtractor field and keyAlias field can not coexist.");
    }

    Map<String, FeatureConfig> features = getFeatures(config);
    AnchorConfigWithExtractor anchorConfig =
        new AnchorConfigWithExtractor(source, keyExtractor, typedKey, keyAlias, extractor, features);
    logger.trace("Built AnchorConfigWithExtractor object for anchor " + name);

    return anchorConfig;
  }

  private static String getExtractorClassName(Config config) {
    ConfigValueType valueType = config.getValue(EXTRACTOR).valueType();

    String extractorClassName;
    switch (valueType) {
      case STRING:
        extractorClassName = config.getString(EXTRACTOR);
        break;

      /*
       * Support for legacy/deprecated extractor: {class: "..."}. Ought to be removed.
       */
      case OBJECT:
        extractorClassName = config.getString(EXTRACTOR + ".class");
        break;

      default:
        throw new ConfigBuilderException("Unknown value type " + valueType + " for key " + EXTRACTOR);
    }
    return extractorClassName;
  }

  // Support for legacy/deprecated "transformer" field. Ought to be removed.
  private static String getTransformerClassName(Config config) {
    return config.getString(TRANSFORMER);
  }
}
