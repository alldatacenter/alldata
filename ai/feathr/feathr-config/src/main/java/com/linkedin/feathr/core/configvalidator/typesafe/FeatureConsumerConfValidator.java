package com.linkedin.feathr.core.configvalidator.typesafe;

import com.linkedin.feathr.core.config.ConfigType;
import com.linkedin.feathr.core.config.consumer.JoinConfig;
import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.config.producer.sources.SourceConfig;
import com.linkedin.feathr.core.config.producer.sources.SourcesConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilder;
import com.linkedin.feathr.core.configdataprovider.ConfigDataProvider;
import com.linkedin.feathr.core.configvalidator.ConfigValidationException;
import com.linkedin.feathr.core.configvalidator.ConfigValidator;
import com.linkedin.feathr.core.configvalidator.ValidationResult;
import com.linkedin.feathr.core.configvalidator.ValidationStatus;
import com.linkedin.feathr.core.configvalidator.ValidationType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;


/**
 * Validator specific for Frame feature consumer clients.
 *
 * The validator provides syntax and semantic validation for Frame configs in the Frame feature consumer clients.
 * For instance, it checks the syntax restrictions from Frame libraries. Some examples of semantic validation will
 * be checking if requested features are reachable (feature is said reachable if the feature is defined in anchors
 * section in FeatureDef config, or if it is a derived feature, then the depended features are reachable),
 * and checking if the source used in feature definition is defined.
 *
 */
public class FeatureConsumerConfValidator extends TypesafeConfigValidator {

  /**
   * validate configs for Frame feature consumer
   *
   * @see ConfigValidator#validate(Map, ValidationType)
   */
  @Override
  public Map<ConfigType, ValidationResult> validate(Map<ConfigType, ConfigDataProvider> configTypeWithDataProvider,
      ValidationType validationType) {

    switch (validationType) {
      case SYNTACTIC:
        // reuse default implementation in super class to perform syntax validation
        return super.validate(configTypeWithDataProvider, ValidationType.SYNTACTIC);
      case SEMANTIC:
        return validateSemantics(configTypeWithDataProvider);
      default:
        throw new ConfigValidationException("Unsupported validation type: " + validationType.name());
    }
  }

  /**
   * Perform semantic validations for provided configs:
   * 1. if no FeatureDef config provided, then return empty result, as all semantic validation requires at least
   *    FeatureDef config provided
   * 2. if only FeatureDef config provided, then perform semantic validation for FeatureDef config
   * 3. if Join config provided, then perform semantic validation for Join config, together with the information provided
   *    in FeatureDef config. For instance, check if features requested in Join config are reachable features in
   *    FeatureDef config
   * 4. if FeatureGeneration config provided, then perform semantic validation for FeatureGeneration config, together
   *    with the information provided in FeatureDef config
   */
  private Map<ConfigType, ValidationResult> validateSemantics(Map<ConfigType, ConfigDataProvider> configTypeWithDataProvider) {
    Map<ConfigType, ValidationResult> result = new HashMap<>();

    // edge cases when the input is not valid or is empty
    if (configTypeWithDataProvider == null || configTypeWithDataProvider.isEmpty()) {
      return result;
    }

    ConfigBuilder configBuilder = ConfigBuilder.get();
    Optional<FeatureDefConfig> optionalFeatureDefConfig;
    Optional<String> sourceNameValidationWarnStr;

    if (configTypeWithDataProvider.containsKey(ConfigType.FeatureDef)) {
      // Populate ValidationResult warning string when source name duplicates exist in different feature def configs
      sourceNameValidationWarnStr = validateFeatureDefConfigSourceNames(configTypeWithDataProvider.get(ConfigType.FeatureDef));
      ConfigDataProvider featureDefConfigDataProvider = configTypeWithDataProvider.get(ConfigType.FeatureDef);
      optionalFeatureDefConfig = Optional.of(configBuilder.buildFeatureDefConfig(featureDefConfigDataProvider));
    } else {
      optionalFeatureDefConfig = Optional.empty();
      sourceNameValidationWarnStr = Optional.empty();
    }

    if (configTypeWithDataProvider.containsKey(ConfigType.Join)) {
      ConfigDataProvider joinConfigDataProvider = configTypeWithDataProvider.get(ConfigType.Join);
      JoinConfig joinConfig = configBuilder.buildJoinConfig(joinConfigDataProvider);
      String errMsg = String.join("", "Can not perform semantic validation as the Join config is",
          "provided but the FeatureDef config is missing.");
      FeatureDefConfig featureDefConfig = optionalFeatureDefConfig.orElseThrow(() -> new ConfigValidationException(errMsg));
      result = validateConsumerConfigSemantics(joinConfig, featureDefConfig);

    } else {
      // TODO add feature generation config semantic validation support
      // only perform semantic check for FeatureDef config
      FeatureDefConfig featureDefConfig = optionalFeatureDefConfig.orElseThrow(() -> new ConfigValidationException(
          "Can not perform semantic validation as the FeatureDef config is missing."));
      result.put(ConfigType.FeatureDef, validateSemantics(featureDefConfig));
    }

    if (sourceNameValidationWarnStr.isPresent() && result.containsKey(ConfigType.FeatureDef)) {
      result.put(ConfigType.FeatureDef,
          new ValidationResult(ValidationType.SEMANTIC, ValidationStatus.WARN, sourceNameValidationWarnStr.get()));
    }
    return result;
  }

  /**
   * Validates feature consumer configs semantically. Requires both {@link JoinConfig} and {@link FeatureDefConfig} to be passed in.
   * @param joinConfig {@link JoinConfig}
   * @param featureDefConfig {@link FeatureDefConfig}
   * @return Map of ConfigType and the {@link ValidationResult}
   */
  private Map<ConfigType, ValidationResult> validateConsumerConfigSemantics(JoinConfig joinConfig, FeatureDefConfig featureDefConfig) {
    Map<ConfigType, ValidationResult> validationResultMap = new HashMap<>();
    FeatureDefConfigSemanticValidator featureDefConfSemanticValidator = new FeatureDefConfigSemanticValidator(true, true);
    validationResultMap.put(ConfigType.FeatureDef, featureDefConfSemanticValidator.validate(featureDefConfig));

    JoinConfSemanticValidator joinConfSemanticValidator = new JoinConfSemanticValidator();
    validationResultMap.put(ConfigType.Join, joinConfSemanticValidator.validate(joinConfig,
        featureDefConfSemanticValidator.getFeatureAccessInfo(featureDefConfig)));
    return validationResultMap;
  }

  /**
   * Check that source names are not duplicated across different feature definition configs.
   * If duplicates exist then the optional string will have a value present, if not, then the optional string will be empty.
   *
   * @param configDataProvider a {@link ConfigDataProvider} with the FeatureDefConfig
   * @return {@link Optional<String>}
   */
  private static Optional<String> validateFeatureDefConfigSourceNames(ConfigDataProvider configDataProvider) {
    StringJoiner warnMsgSj = new StringJoiner("\n");
    Set<String> sourcesSet = new HashSet<>();
    Set<String> duplicateSourceNames = new HashSet<>();
    // for each resource, construct a FeatureDefConfig
    ConfigBuilder configBuilder = ConfigBuilder.get();
    List<FeatureDefConfig> builtFeatureDefConfigList = configBuilder.buildFeatureDefConfigList(configDataProvider);

    for (FeatureDefConfig featureDefConfig : builtFeatureDefConfigList) {

      if (featureDefConfig.getSourcesConfig().isPresent()) {
        SourcesConfig source = featureDefConfig.getSourcesConfig().get();
        Map<String, SourceConfig> sources = source.getSources();

        for (String sourceName : sources.keySet()) {
          if (sourcesSet.contains(sourceName)) {
            duplicateSourceNames.add(sourceName);
          } else {
            sourcesSet.add(sourceName);
          }
        }
      }
    }

    if (duplicateSourceNames.size() > 0) {
      warnMsgSj.add("The following source name(s) are duplicates between two or more feature definition configs: ");
      for (String entry : duplicateSourceNames) {
        warnMsgSj.add("source name: " + entry);
      }
      warnMsgSj.add("File paths of two or more files that have duplicate source names: \n" + configDataProvider.getConfigDataInfo());
    }

    String warnMsg = warnMsgSj.toString();
    Optional<String> returnString = warnMsg.isEmpty() ? Optional.empty() : Optional.of(warnMsg);

    return returnString;
  }

  /**
   * Validates FeatureDef config semantically
   * @param featureDefConfig {@link FeatureDefConfig}
   * @return {@link ValidationResult}
   */
  @Override
  public ValidationResult validateSemantics(FeatureDefConfig featureDefConfig) {
    return new FeatureDefConfigSemanticValidator(true, true).validate(featureDefConfig);
  }
}
