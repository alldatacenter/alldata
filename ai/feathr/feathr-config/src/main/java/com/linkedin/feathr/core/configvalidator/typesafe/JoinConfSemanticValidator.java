package com.linkedin.feathr.core.configvalidator.typesafe;

import com.linkedin.feathr.core.configvalidator.ValidationResult;
import com.linkedin.feathr.core.config.consumer.JoinConfig;
import com.linkedin.feathr.core.configvalidator.ValidationStatus;
import com.linkedin.feathr.core.configvalidator.ValidationType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;


/**
 * package private validator class specific for Join config semantic validation
 */
class JoinConfSemanticValidator {

  /**
   * semantic validation for Join config
   * @param joinConfig the {@link JoinConfig}
   * @param featureReachableInfo feature reachable information extracted from FeatureDef config
   */
  ValidationResult validate(JoinConfig joinConfig, Map<FeatureReachType, Set<String>> featureReachableInfo) {

    Set<String> requestedFeatureNames = getRequestedFeatureNames(joinConfig);

    // get reachable features defined in FeatureDef config
    Set<String> reachableFeatureNames = featureReachableInfo.getOrDefault(FeatureReachType.REACHABLE,
        Collections.emptySet());
    // get unreachable features defined in FeatureDef config
    Set<String> unreachableFeatureNames = featureReachableInfo.getOrDefault(FeatureReachType.UNREACHABLE,
        Collections.emptySet());

    // requested features that are not defined
    Set<String> undefinedRequestedFeatures = new HashSet<>();

    /*
     * requested features that are defined in FeatureDef config, but these features are in fact not reachable
     * For instance, the requested features can be defined in "derivations" section, but the derived feature might
     *  not be reachable because its depended features might not be reachable
     */
    Set<String> unreachableRequestedFeatures = new HashSet<>();

    requestedFeatureNames.stream().filter(f -> !reachableFeatureNames.contains(f)).forEach(f -> {
      if (unreachableFeatureNames.contains(f)) {
        unreachableRequestedFeatures.add(f);
      } else {
        undefinedRequestedFeatures.add(f);
      }
    });

    return constructRequestedFeaturesValidationResult(undefinedRequestedFeatures, unreachableRequestedFeatures);
  }

  /**
   * construct final ValidationResult based on the found undefined requested features, and unreachable requested features
   */
  private ValidationResult constructRequestedFeaturesValidationResult(Set<String> undefinedRequestedFeatures,
      Set<String> unreachableRequestedFeatures) {
    if (undefinedRequestedFeatures.isEmpty() && unreachableRequestedFeatures.isEmpty()) {
      return ValidationResult.VALID_SEMANTICS;
    }

    StringJoiner errMsgJoiner = new StringJoiner("\n");
    if (!undefinedRequestedFeatures.isEmpty()) {
      String tipMsg = String.join("", "The following requested features are not defined.",
          " It could be possible that 1) typos in feature name, 2) feature definition is not included: ");
      errMsgJoiner.add(tipMsg);
      undefinedRequestedFeatures.forEach(errMsgJoiner::add);
    }

    if (!unreachableRequestedFeatures.isEmpty()) {
      String tipMsg = String.join("", "The following requested features are unreachable",
          " features defined in FeatureDef. This is usually due to incorrect feature definition: ");
      errMsgJoiner.add(tipMsg);
      unreachableRequestedFeatures.forEach(errMsgJoiner::add);
    }

    return new ValidationResult(ValidationType.SEMANTIC, ValidationStatus.INVALID, errMsgJoiner.toString());
  }

  // static method get all requested features in the Join config, by merging requested features in each FeatureBag
  static Set<String> getRequestedFeatureNames(JoinConfig joinConfig) {
    return joinConfig.getFeatureBagConfigs().entrySet().stream()
        .flatMap(entry -> entry.getValue().getKeyedFeatures().stream().flatMap(f -> f.getFeatures().stream()))
        .collect(Collectors.toSet());
  }
}
