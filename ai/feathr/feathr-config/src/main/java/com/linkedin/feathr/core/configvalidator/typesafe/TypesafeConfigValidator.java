package com.linkedin.feathr.core.configvalidator.typesafe;

import com.linkedin.feathr.core.configvalidator.ValidationResult;
import com.linkedin.feathr.core.config.ConfigType;
import com.linkedin.feathr.core.config.consumer.JoinConfig;
import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.configbuilder.typesafe.TypesafeConfigBuilder;
import com.linkedin.feathr.core.configdataprovider.ConfigDataProvider;
import com.linkedin.feathr.core.configvalidator.ConfigValidationException;
import com.linkedin.feathr.core.configvalidator.ConfigValidator;
import com.linkedin.feathr.core.configvalidator.ValidationType;
import com.linkedin.feathr.core.utils.Utils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;

import static com.linkedin.feathr.core.config.producer.FeatureDefConfig.*;
import static com.linkedin.feathr.core.config.producer.anchors.AnchorConfig.FEATURES;
import static com.linkedin.feathr.core.configvalidator.ValidationStatus.*;
import static com.linkedin.feathr.core.configvalidator.ValidationType.*;


/**
 * @deprecated package private use only, please use {@link FeatureConsumerConfValidator} or
 * {@link FeatureProducerConfValidator} as needed
 *
 * This class implements {@link ConfigValidator} using the Lightbend (aka Typesafe) Config Library.
 * Also provides config validation methods that operate on Typesafe Config objects instead of a
 * {@link ConfigDataProvider}. These methods will be used by {@link TypesafeConfigBuilder} during
 * config building.
 */
@Deprecated
public class TypesafeConfigValidator implements ConfigValidator {
  private static final Logger logger = LogManager.getLogger(TypesafeConfigValidator.class);

  // Used when rendering the parsed config to JSON string (which is then used in validation)
  private ConfigRenderOptions _renderOptions;

  private final static String FEATUREDEF_CONFIG_SCHEMA = "/FeatureDefConfigSchema.json";

  private final static String JOIN_CONFIG_SCHEMA = "/JoinConfigSchema.json";

  private final static String PRESENTATION_CONFIG_SCHEMA = "/PresentationsConfigSchema.json";

  private static final String ANCHOR_SOURCE_NAME_REGEX = "(^[a-zA-Z][-\\w]*$)";
  private static final Pattern ANCHOR_SOURCE_NAME_PATTERN = Pattern.compile(ANCHOR_SOURCE_NAME_REGEX);

  /*
   * We use the following four fields to name the capturing groups, for ease of use
   */
  private static final String NAMESPACE = "namespace";
  private static final String NAME = "name";
  private static final String MAJOR = "major";
  private static final String MINOR = "minor";

  /*
   * The delimiter used to separate namespace, name and version fields. It must be chosen such that it doesn't
   * conflict with the restricted characters used in HOCON, Pegasus's PathSpec and the characters used in Java
   * variable names.
   */
  public static final String DELIM = "-";

  // BNF of the typed ref is: (namespace-)?name(-major-minor)?
  public static final String TYPED_REF_BNF = String .join(DELIM, "(namespace", ")?name(", "major", "minor)?");

  /*
   * For all of the regex's below, the outer group where applicable, is made non-capturing by using "?:" construct.
   * This is done since we want to extract only "foo" in "foo-". Also, we use named-capturing groups by using "?<name>"
   * construct. This is done for ease of reference when getting the matched value of the group.
   */

  // Represents the regex for (namespace-)?
  private static final String NAMESPACE_REGEX = "(?:(?<" + NAMESPACE + ">[a-zA-Z][\\w]+)" + DELIM + ")?";

  // Represents the regex for name
  // Note: We shouldn't allow '.' or ':' in name, but in some legacy feature names, "." or ":" are being used.
  // Build validation project will gradually migrate these legacy feature names off from using special characters,
  // when a clean state is reached, we should remove these special characters from the regex.
  private static final String NAME_REGEX = "(?<" + NAME + ">[a-zA-Z][.:\\w]*)";
  private static final String STRICT_NAME_REGEX = "(?<" + NAME + ">[a-zA-Z][\\w]*)";

  // Represents the regex for only feature name
  private static final String FEATURE_NAME_REGEX = "([a-zA-Z][.:\\w]*)";

  // Represents regex for (-major-minor)?
  private static final String VERSION_REGEX = "((?:" + DELIM + "(?<" + MAJOR + ">[\\d]+))(?:" + DELIM + "(?<" + MINOR + ">[\\d]+)))?";

  private static final String TYPED_REF_REGEX = NAMESPACE_REGEX + NAME_REGEX + VERSION_REGEX;

  private static final String STRICT_TYPED_REF_REGEX = "^" + NAMESPACE_REGEX + STRICT_NAME_REGEX + VERSION_REGEX + "$";
  public static final Pattern STRICT_TYPED_REF_PATTERN = Pattern.compile(STRICT_TYPED_REF_REGEX);

  public TypesafeConfigValidator() {
    _renderOptions = ConfigRenderOptions.defaults()
        .setComments(false)
        .setOriginComments(false)
        .setFormatted(true)
        .setJson(true);
  }

  /**
   * @see ConfigValidator#validate(ConfigType, ValidationType, ConfigDataProvider)
   */
  @Override
  public ValidationResult validate(ConfigType configType, ValidationType validationType,
      ConfigDataProvider configDataProvider) {
    ValidationResult result;

    switch (validationType) {
      case SYNTACTIC:
        // First build a Typesafe Config object representation
        Config config;
        try {
          config = buildTypesafeConfig(configType, configDataProvider);
        } catch (ConfigException e) {
          String details = "Config parsing failed due to invalid HOCON syntax";
          result = new ValidationResult(SYNTACTIC, INVALID, details, e);
          break;
        }

        // Delegate syntax validation to another method
        result = validateSyntax(configType, config);
        break;

      case SEMANTIC:
        result = validateSemantics(configType, configDataProvider);
        break;

      default:
        throw new ConfigValidationException("Unsupported validation type " + validationType);
    }
    logger.info("Performed " + validationType + " validation for " + configType + " config from "
        + configDataProvider.getConfigDataInfo());

    return result;

  }

  /**
   * @see ConfigValidator#validate(Map, ValidationType)
   */
  @Override
  public Map<ConfigType, ValidationResult> validate(Map<ConfigType, ConfigDataProvider> configTypeWithDataProvider,
      ValidationType validationType) {
    Map<ConfigType, ValidationResult> resultMap = new HashMap<>();

    for (Map.Entry<ConfigType, ConfigDataProvider> entry : configTypeWithDataProvider.entrySet()) {
      ConfigType configType = entry.getKey();
      ConfigDataProvider configDataProvider = entry.getValue();
      ValidationResult result = validate(configType, validationType, configDataProvider);
      resultMap.put(configType, result);
    }

    return resultMap;
  }

  /**
   * Validates the configuration syntax. Configuration type is provided by {@link ConfigType}, and the configuration
   * to be validated is provided by {@link Config} object
   * @param configType ConfigType
   * @param config Config object
   * @return {@link ValidationResult}
   * @throws ConfigValidationException if validation can't be performed
   */
  public ValidationResult validateSyntax(ConfigType configType, Config config) {
    ValidationResult result;

    /*
     * Creates a JSON string from the HOCON config object, and validates the syntax of the config string as a valid
     * Frame config (FeatureDef or Join).
     */
    try {
      String jsonStr = config.root().render(_renderOptions);

      JSONTokener tokener = new JSONTokener(jsonStr);
      JSONObject root = new JSONObject(tokener);

      switch (configType) {
        case FeatureDef:
          // validate naming convention
          result = validateFeatureDefNames(config);
          break;

        case Join:
          result = new ValidationResult(SYNTACTIC, VALID);
          break;

        case Presentation:
          result = new ValidationResult(SYNTACTIC, VALID);
          break;
        default:
          throw new ConfigValidationException("Unknown config type: " + configType);
      }
    } catch (ConfigValidationException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigValidationException("Config validation error", e);
    }
    logger.debug("Validated " + configType + " config syntax");

    return result;
  }

  /**
   * Validates FeatureDef config semantically. Intended to be used by TypesafeConfigBuilder.
   * @param featureDefConfig {@link FeatureDefConfig}
   * @return {@link ValidationResult}
   */
  public ValidationResult validateSemantics(FeatureDefConfig featureDefConfig) {
    return new FeatureDefConfigSemanticValidator().validate(featureDefConfig);
  }

  /**
   * Validates Join config semantically. Requires both {@link JoinConfig} and {@link FeatureDefConfig} to be passed in.
   * @param joinConfig {@link JoinConfig}
   * @param featureDefConfig {@link FeatureDefConfig}
   * @return {@link ValidationResult}
   */
  public ValidationResult validateSemantics(JoinConfig joinConfig, FeatureDefConfig featureDefConfig) {
    throw new ConfigValidationException("Join config semantic validation not yet implemented!");
  }

  private ValidationResult validateSemantics(ConfigType configType, ConfigDataProvider configDataProvider) {
    ValidationResult result;

    switch (configType) {
      case FeatureDef:
        result = validateFeatureDefConfigSemantics(configDataProvider);
        break;

      case Join:
        result = validateJoinConfigSemantics(configDataProvider);
        break;

      default:
        throw new  ConfigValidationException("Unsupported config type " + configType);
    }

    return result;
  }

  private ValidationResult validateFeatureDefConfigSemantics(ConfigDataProvider configDataProvider) {
    try {
      TypesafeConfigBuilder typesafeConfigBuilder = new TypesafeConfigBuilder();
      FeatureDefConfig featureDefConfig = typesafeConfigBuilder.buildFeatureDefConfig(configDataProvider);
      return validateSemantics(featureDefConfig);
    } catch (Throwable e) {
      throw new ConfigValidationException("Fail to perform semantic validation for FeatureDef config with"
          + configDataProvider.getConfigDataInfo(), e);
    }
  }

  private ValidationResult validateJoinConfigSemantics(ConfigDataProvider configDataProvider) {
    /*
     * TODO: To semantically validate a Join Config, we'll need both Join and FeatureDef configs. This will
     *  require changes to ConfigDataProvider interface which should have methods for getting config data
     *  separately for FeatureDef config, Join config, etc.
     *  Once obtained as above, build Frame's FeatureDefConfig and JoinConfig objects, and perform semantic
     *  validation. So,
     *  1. Invoke TypesafeConfigBuilder to build FeatureDefConfig object.
     *  2. Invoke TypesafeConfigBuilder to build JoinConfig object.
     *  3. Invoke #validateSemantics(JoinConfig joinConfig, FeatureDefConfig featureDefConfig)
     */
    throw new ConfigValidationException("Join config semantic validation not yet implemented!");
  }

  /**
   * validate defined source name, anchor name, feature name in typesafe FeatureDef config
   */
  private ValidationResult validateFeatureDefNames(Config config) {
    Set<String> definedSourceAnchorNames = new HashSet<>();
    Set<String> definedFeatureNames = new HashSet<>();

    if (config.hasPath(SOURCES)) { // add all source names
      definedSourceAnchorNames.addAll(config.getConfig(SOURCES).root().keySet());
    }

    if (config.hasPath(ANCHORS)) {
      Config anchorsCfg = config.getConfig(ANCHORS);
      Set<String> anchorNames = anchorsCfg.root().keySet();
      definedSourceAnchorNames.addAll(anchorNames); // add all anchor names

      // add all anchor defined feature names
      anchorNames.stream().map(Utils::quote).forEach(quotedName ->
          definedFeatureNames.addAll(getFeatureNamesFromAnchorDef(anchorsCfg.getConfig(quotedName)))
      );
    }

    if (config.hasPath(DERIVATIONS)) { // add all derived feature names
      definedFeatureNames.addAll(config.getConfig(DERIVATIONS).root().keySet());
    }

    definedSourceAnchorNames.removeIf(name -> ANCHOR_SOURCE_NAME_PATTERN.matcher(name).find());
    definedFeatureNames.removeIf(name -> STRICT_TYPED_REF_PATTERN.matcher(name).find());

    return constructNamingValidationResult(definedSourceAnchorNames, definedFeatureNames);
  }

  /**
   * construct naming convention check validation result for invalid names
   */
  private ValidationResult constructNamingValidationResult(Set<String> invalidSourceAnchorNames,
      Set<String> invalidFeatureNames) {

    if (invalidFeatureNames.isEmpty() && invalidSourceAnchorNames.isEmpty()) {
      return new ValidationResult(SYNTACTIC, VALID);
    }

    StringJoiner sj = new StringJoiner("\n", "", "\n");

    if (!invalidFeatureNames.isEmpty()) {
      String msg = String.join("\n",
          "The feature references/names in Frame configs must conform to the pattern (shown in BNF syntax): "
              + TYPED_REF_BNF
              + ", where the 'name' must conform to the pattern (shown as regex) [a-zA-Z][\\w]+",
          "The following names violate Frame's feature naming convention: ",
          String.join("\n", invalidFeatureNames)
      );
      sj.add(msg);
    }

    if (!invalidSourceAnchorNames.isEmpty()) {
      String msg = String.join("\n",
          "The source and anchor names in Frame configs follow the pattern (shown as regex) "
              + ANCHOR_SOURCE_NAME_REGEX,
          "The following names violate Frame's source and anchor naming convention: ",
          String.join("\n", invalidSourceAnchorNames)
      );
      sj.add(msg);
    }

    return new ValidationResult(SYNTACTIC, WARN, sj.toString());
  }

  /**
   * get feature names from typesafe config with anchor definition
   */
  private Set<String> getFeatureNamesFromAnchorDef(Config anchorConfig) {

    ConfigValue value = anchorConfig.getValue(FEATURES);
    ConfigValueType valueType = value.valueType();

    Set<String> featureNames;
    switch (valueType) { // Note that features can be expressed as a list or as an object
      case LIST:
        featureNames = new HashSet<>(anchorConfig.getStringList(FEATURES));
        break;

      case OBJECT:
        featureNames = anchorConfig.getConfig(FEATURES).root().keySet();
        break;

      default:
        StringBuilder sb = new StringBuilder();
        sb.append("Fail to extract feature names from anchor config. ").append("Expected ")
            .append(FEATURES).append(" value type List or Object, got ").append(valueType.toString());
        throw new RuntimeException(sb.toString());
    }

    return featureNames;
  }

  private Config buildTypesafeConfig(ConfigType configType, ConfigDataProvider configDataProvider) {
    TypesafeConfigBuilder builder = new TypesafeConfigBuilder();
    return builder.buildTypesafeConfig(configType, configDataProvider);
  }
}
