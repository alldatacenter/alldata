package com.linkedin.feathr.core.configvalidator.typesafe;

import com.google.common.collect.ImmutableSet;
import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfig;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfigWithExtractor;
import com.linkedin.feathr.core.config.producer.anchors.ExtractorBasedFeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.FeatureConfig;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfig;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfigWithExpr;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfigWithExtractor;
import com.linkedin.feathr.core.config.producer.derivations.DerivationsConfig;
import com.linkedin.feathr.core.config.producer.derivations.KeyedFeature;
import com.linkedin.feathr.core.config.producer.derivations.SequentialJoinConfig;
import com.linkedin.feathr.core.config.producer.derivations.SimpleDerivationConfig;
import com.linkedin.feathr.core.configvalidator.ValidationResult;
import com.linkedin.feathr.core.configvalidator.ValidationStatus;
import com.linkedin.feathr.core.configvalidator.ValidationType;
import com.linkedin.feathr.exception.ErrorLabel;
import com.linkedin.feathr.exception.FeathrConfigException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.linkedin.feathr.core.configvalidator.typesafe.FeatureReachType.*;


/**
 * validator specific for FeatureDef config validation
 */
class FeatureDefConfigSemanticValidator {

  // Represents the regex for only feature name
  private static final String FEATURE_NAME_REGEX = "([a-zA-Z][.:\\w]*)";
  public static final Pattern FEATURE_NAME_PATTERN = Pattern.compile(FEATURE_NAME_REGEX);

  private boolean _withFeatureReachableValidation;
  private boolean _withUndefinedSourceValidation;
  // Anchors with parameters can only be used with approval. The following set is the allowed extractors.
  // Adding a first allowed dummy extractor for testing.
  // TODO - 17349): Add Galene's parameterized extractors.
  private static final Set<String> ALLOWED_EXTRACTOR_WITH_PARAMETERS = ImmutableSet.of(
      "com.linkedin.feathr.SampleExtractorWithParams",
      // For feed use cases, key tags themselves are also used as features, such as actorUrn, objectUrn etc. This
      // extractor is to extract features from key tags.
      "com.linkedin.followfeed.feathr.extractor.KeyTagFeatureExtractor");

  /**
   * constructor
   * @param withFeatureReachableValidation flag to perform feature reachable validation
   * @param withUndefinedSourceValidation flag to perform undefined source validation
   */
  FeatureDefConfigSemanticValidator(boolean withFeatureReachableValidation, boolean withUndefinedSourceValidation) {
    _withFeatureReachableValidation = withFeatureReachableValidation;
    _withUndefinedSourceValidation = withUndefinedSourceValidation;
  }

  /**
   * constructor
   */
  FeatureDefConfigSemanticValidator() {
    _withFeatureReachableValidation = false;
    _withUndefinedSourceValidation = false;
  }

  /**
   * the entry for FeatureDef config semantic validation
   */
  ValidationResult validate(FeatureDefConfig featureDefConfig) {
    validateApprovedExtractorWithParameters(featureDefConfig);

    StringJoiner warnMsgSj = new StringJoiner("\n"); // concat all warning messages together and output
    int  warnMsgSjInitLength = warnMsgSj.length(); // get the init length of the warning message,

    try {
      // check duplicate feature names
      Set<String> duplicateFeatures = getDuplicateFeatureNames(featureDefConfig);
      if (!duplicateFeatures.isEmpty()) {
        String warnMsg = String.join("\n", "The following features' definitions are duplicate: ",
            String.join("\n", duplicateFeatures));
        warnMsgSj.add(warnMsg);
      }

      // check if all sources used in anchors are defined
      if (_withUndefinedSourceValidation) {
        Map<String, String> undefinedAnchorSources = getUndefinedAnchorSources(featureDefConfig);
        if (!undefinedAnchorSources.isEmpty()) {
          StringJoiner sj = new StringJoiner("\n");
          for (Map.Entry<String, String> entry : undefinedAnchorSources.entrySet()) {
            sj.add(String.join(" ", "Source", entry.getValue(), "used in anchor", entry.getKey(), "is not defined."));
          }
          return new ValidationResult(ValidationType.SEMANTIC, ValidationStatus.INVALID, sj.toString());
        }
      }

      /*
       * check if all input features for derived features are reachable
       * This can only be a warning here as the features might not be required
       */
      if (_withFeatureReachableValidation) {
        Map<FeatureReachType, Set<String>> featureAccessInfo = getFeatureAccessInfo(featureDefConfig);
        Set<String> unreachableFeatures = featureAccessInfo.getOrDefault(UNREACHABLE, Collections.emptySet());
        if (!unreachableFeatures.isEmpty()) {
          String warnMsg = String.join("", "The following derived features cannot be computed as ",
              "one or more of their ancestor features cannot be found:\n", String.join("\n", unreachableFeatures));
          warnMsgSj.add(warnMsg);
        }
      }

      /*
       * dedicate to MvelValidator for MVEL expression validation
       */
      MvelValidator mvelValidator = MvelValidator.getInstance();
      ValidationResult mvelValidationResult = mvelValidator.validate(featureDefConfig);
      if (mvelValidationResult.getValidationStatus() == ValidationStatus.WARN) {
        warnMsgSj.add(mvelValidationResult.getDetails().orElse(""));
      }

      /*
       * validate HDFS sources
       */
      HdfsSourceValidator hdfsSourceValidator = HdfsSourceValidator.getInstance();
      ValidationResult hdfsSourceValidationResult = hdfsSourceValidator.validate(featureDefConfig);
      if (hdfsSourceValidationResult.getValidationStatus() == ValidationStatus.WARN) {
        warnMsgSj.add(hdfsSourceValidationResult.getDetails().orElse(""));
      } else if (hdfsSourceValidationResult.getValidationStatus() == ValidationStatus.INVALID) {
        return hdfsSourceValidationResult;
      }

    } catch (Throwable e) {
      return new ValidationResult(ValidationType.SEMANTIC, ValidationStatus.PROCESSING_ERROR, e.getMessage(), e);
    }

    /*
     * If new warning message is added, return a warning validation result,
     * else, return a valid validation result
     */
    return warnMsgSj.length() > warnMsgSjInitLength
        ? new ValidationResult(ValidationType.SEMANTIC, ValidationStatus.WARN, warnMsgSj.toString())
        : new ValidationResult(ValidationType.SEMANTIC, ValidationStatus.VALID);
  }

  /**
   * Validate that feature params is only allowed to be used by approved use cases. Here we use extractor name to target
   * the approved use cases.
   */
  void validateApprovedExtractorWithParameters(FeatureDefConfig featureDefConfig) {
    for (Map.Entry<String, AnchorConfig> entry : featureDefConfig.getAnchorsConfig().get().getAnchors().entrySet()) {
      AnchorConfig anchorConfig = entry.getValue();
      for (Map.Entry<String, FeatureConfig> featureEntry : anchorConfig.getFeatures().entrySet()) {
        FeatureConfig featureConfig = featureEntry.getValue();
        if (featureConfig instanceof ExtractorBasedFeatureConfig && !featureConfig.getParameters().isEmpty()) {
          if (anchorConfig instanceof AnchorConfigWithExtractor) {
            String extractor = ((AnchorConfigWithExtractor) anchorConfig).getExtractor();
            if (!ALLOWED_EXTRACTOR_WITH_PARAMETERS.contains(extractor)) {
              throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, "anchorConfig: " + anchorConfig
                  + " has parameters. Parameters are only approved to be used by the following extractors: "
                  + ALLOWED_EXTRACTOR_WITH_PARAMETERS);
            }
          } else {
            // If it's not AnchorConfigWithExtractor but it has parameters, it's not allowed.
            throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR,
                "Parameters are only to be used by AnchorConfigWithExtractor. The anchor config is: "
                    + anchorConfig);
          }
        }
      }
    }
  }

  /**
   * Semantic check, get all the anchors whose source is not defined
   * @param featureDefConfig {@link FeatureDefConfig} object
   * @return mapping of anchor name to the undefined source name
   */
  Map<String, String> getUndefinedAnchorSources(FeatureDefConfig featureDefConfig) {
    Map<String, String> undefinedAnchorSource = new HashMap<>();
    Set<String> definedSourceNames = getDefinedSourceNames(featureDefConfig);
    // if an anchor's source is not defined, then return the mapping from anchor name to source name
    BiConsumer<String, AnchorConfig> consumeAnchor = (anchorName, anchorConfig) -> {
      String sourceName = anchorConfig.getSource();
      /*
       * Here sourceName can be file path in Frame offline, in which case it is not defined in sources section.
       * The source defined in sources section can not contain special char / and ., which can be used to distinguish
       *   source definition from file path.
       */
      if (!(sourceName.contains("/") || sourceName.contains("."))) {
        if (!definedSourceNames.contains(sourceName)) {
          undefinedAnchorSource.put(anchorName, sourceName);
        }
      }
    };

    featureDefConfig.getAnchorsConfig().ifPresent(anchorsConfig ->
        anchorsConfig.getAnchors().forEach(consumeAnchor)
    );
    return undefinedAnchorSource;
  }

  /**
   * get all defined source names
   * @param featureDefConfig {@link FeatureDefConfig} object
   * @return set of all defined source names
   */
  private Set<String> getDefinedSourceNames(FeatureDefConfig featureDefConfig) {
    Set<String> definedSourceNames = new HashSet<>();
    featureDefConfig.getSourcesConfig().ifPresent(sourcesConfig ->
        definedSourceNames.addAll(sourcesConfig.getSources().keySet()));
    return definedSourceNames;
  }

  /**
   * get duplicate features defined in FeatureDefConfig
   * @param featureDefConfig {@link FeatureDefConfig} object, the object should be built from single config file
   */
  Set<String> getDuplicateFeatureNames(FeatureDefConfig featureDefConfig) {
    Set<String> definedFeatures = new HashSet<>();
    Set<String> duplicateFeatures = new HashSet<>();

    // check if there is duplicate features in multiple anchors
    BiConsumer<String, AnchorConfig> checkAnchor = (anchorName, anchorConfig) -> {
      Set<String> features = anchorConfig.getFeatures().keySet();
      for (String feature: features) {
        if (definedFeatures.contains(feature)) {
          duplicateFeatures.add(feature);
        }
        definedFeatures.add(feature);
      }
    };

    featureDefConfig.getAnchorsConfig().ifPresent(anchorsConfig -> {
      anchorsConfig.getAnchors().forEach(checkAnchor);
    });

    // check if there is duplicate features defined in both derivations and above anchors
    BiConsumer<String, DerivationConfig> checkDerivation = (featureName, derivationConfig) -> {
      if (definedFeatures.contains(featureName)) {
        duplicateFeatures.add(featureName);
      }
      definedFeatures.add(featureName);
    };

    featureDefConfig.getDerivationsConfig().ifPresent(derivationsConfig -> {
      derivationsConfig.getDerivations().forEach(checkDerivation);
    });

    return duplicateFeatures;
  }


  /**
   * Get all required features from a set of requested features.
   * Definition:
   * A feature is a required feature if it is a requested feature, or it is a depended feature of a required derive feature.
   *
   * Note, this can also be achieved with the dependency graph built with frame-common library. However,
   * frame-core can not depend on frame-common to avoid a circular dependency. Here we implement a lighter version
   * of dependency graph with only feature names to get required feature names.
   *
   * @param featureDefConfig {@link FeatureDefConfig} object
   * @param requestedFeatureNames set of requested feature names
   * @return set of required feature names
   */
  static Set<String> getRequiredFeatureNames(FeatureDefConfig featureDefConfig, Set<String> requestedFeatureNames) {
    Set<String> requiredFeatureNames = new HashSet<>();
    // put requested feature names into a queue, and resolve its dependency with BFS
    Queue<String> featuresToResolve = new LinkedList<>(requestedFeatureNames);

    Map<String, Set<String>> dependencyGraph = getDependencyGraph(featureDefConfig);
    // BFS to find all required feature names in the dependency graph
    while (!featuresToResolve.isEmpty()) {
      String feature = featuresToResolve.poll();
      requiredFeatureNames.add(feature);
      dependencyGraph.getOrDefault(feature, Collections.emptySet()).forEach(featuresToResolve::offer);
    }

    return requiredFeatureNames;
  }

  /**
   * Get all anchored feature names, which are considered reachable directly.
   * See the definition of "reachable" in {@link #getFeatureAccessInfo(FeatureDefConfig)}.
   * @param featureDefConfig {@link FeatureDefConfig} object
   * @return set of anchored feature names
   */
  private static Set<String> getAnchoredFeatureNames(FeatureDefConfig featureDefConfig) {
    Set<String> anchoredFeatures = new HashSet<>();

    featureDefConfig.getAnchorsConfig().ifPresent(anchorsConfig -> {
      Set<String> features = anchorsConfig.getAnchors().entrySet().stream()
          .flatMap(x -> x.getValue().getFeatures().keySet().stream()).collect(Collectors.toSet());
      anchoredFeatures.addAll(features);
    });

    return anchoredFeatures;
  }

  /**
   * Get all reachable and unreachable feature names in the input FeatureDef config.
   * Here a feature is reachable if and only if the feature is defined in anchors section, or
   * its depend features (a.k.a input features or base features) are all reachable.
   * @param featureDefConfig {@link FeatureDefConfig} object
   * @return all reachable and unreachable feature names
   */
  Map<FeatureReachType, Set<String>> getFeatureAccessInfo(FeatureDefConfig featureDefConfig) {
    Set<String> reachableFeatures = getAnchoredFeatureNames(featureDefConfig);

    Map<String, DerivationConfig> derivations = featureDefConfig.getDerivationsConfig().
        orElse(new DerivationsConfig(Collections.emptyMap())).getDerivations();
    Set<String> allDerivedFeatures = derivations.keySet();

    // get all defined features in "anchors" section, and "derivations" section.
    Set<String> allDefinedFeatures = new HashSet<>(reachableFeatures);
    allDefinedFeatures.addAll(allDerivedFeatures);

    Set<String> unreachableFeatures = new HashSet<>();
    // recursively find all reachable and unreachable features
    for (String derivedFeature: derivations.keySet()) {
      checkFeatureReachable(reachableFeatures, unreachableFeatures, derivations, allDefinedFeatures, derivedFeature);
    }

    Map<FeatureReachType, Set<String>> features = new HashMap<>();
    features.put(REACHABLE, reachableFeatures);
    features.put(UNREACHABLE, unreachableFeatures);
    return features;
  }

  /**
   * Recursive call to check if a query feature is reachable, collect all reachable and unreachable features during the
   *   recursive processes(side effect).
   * See the definition of "reachable" in {@link #getFeatureAccessInfo(FeatureDefConfig)}.
   * @param reachableFeatures all known reachable features
   * @param unreachableFeatures all features that are not reachable
   * @param derivations derived feature name mapping to its definition as {@link DerivationConfig} obj
   * @param allDefinedFeatures all defined feature names in "anchors" and "derivations" section
   * @param queryFeature the query feature
   * @return if the query feature is reachable (boolean)
   */
  private boolean checkFeatureReachable(Set<String> reachableFeatures,
      Set<String> unreachableFeatures,
      Map<String, DerivationConfig> derivations,
      Set<String> allDefinedFeatures,
      String queryFeature) {

    boolean featureReachable = true;
    // base case, we've already known if the query feature is reachable or not
    if (reachableFeatures.contains(queryFeature)) {
      return true;
    } else if (unreachableFeatures.contains(queryFeature)) {
      return false;
    } else if (!derivations.containsKey(queryFeature)) {
      /*
       * Since all anchored features are considered as reachable features,
       * if the feature is not a known reachable feature, then it is not a anchored feature.
       * It is also not defined in derivation, then it is a undefined feature, and should be considered as
       * unreachable.
       */
      featureReachable = false;
    } else {
      /*
       * If the feature is not directly reachable, check if all the dependencies are reachable
       * Do not stop the recursive call when finding the first unreachable feature, instead collect all the features
       *  that are not reachable in one shot.
       */
      for (String baseFeature: getInputFeatures(queryFeature, derivations.get(queryFeature), allDefinedFeatures)) {
        if (!checkFeatureReachable(reachableFeatures, unreachableFeatures, derivations, allDefinedFeatures, baseFeature)) {
          featureReachable = false;
        }
      }
    }

    //collect reachable and unreachable features
    if (featureReachable) {
      reachableFeatures.add(queryFeature);
    } else {
      unreachableFeatures.add(queryFeature);
    }

    return featureReachable;
  }

  /**
   * a light version feature name dependency graph represented by adjacent list(set),
   * where the key is a feature name, and the value is the set of features the keyed-feature depends on.
   * If the feature is a anchored feature, then the depended feature set is EMPTY.
   */
  private static Map<String, Set<String>> getDependencyGraph(FeatureDefConfig featureDefConfig) {
    Map<String, Set<String>> dependencyGraph = new HashMap<>();
    Set<String> anchoredFeatures = getAnchoredFeatureNames(featureDefConfig);
    anchoredFeatures.forEach(f -> dependencyGraph.put(f, Collections.emptySet()));

    Map<String, DerivationConfig> derivations = featureDefConfig.getDerivationsConfig().
        orElse(new DerivationsConfig(Collections.emptyMap())).getDerivations();
    Set<String> allDerivedFeatures = derivations.keySet();

    Set<String> allDefinedFeatures = new HashSet<>(anchoredFeatures);
    allDefinedFeatures.addAll(allDerivedFeatures);

    derivations.forEach((k, v) -> dependencyGraph.put(k, getInputFeatures(k, v, allDefinedFeatures)));

    return dependencyGraph;
  }

  /**
   * get input features of a derived feature from {@link DerivationConfig} obj
   * @param derivedFeature derived feature name
   * @param derivationConfig derived feature {@link DerivationConfig} obj
   * @param allDefinedFeatureNames all defined feature names, this is considered as reference to extract input features
   *                              if input features are defined in MVEL expression
   * @return set of input feature names
   */
  private static Set<String> getInputFeatures(String derivedFeature,
      DerivationConfig derivationConfig,
      Set<String> allDefinedFeatureNames) {

    Set<String> inputs; // all the base/input keyed features
    if (derivationConfig instanceof DerivationConfigWithExpr) {
      DerivationConfigWithExpr derivationConfigWithExpr = (DerivationConfigWithExpr) derivationConfig;
      inputs = derivationConfigWithExpr.getInputs().values().stream().map(KeyedFeature::getFeature).
          collect(Collectors.toSet());
    } else if (derivationConfig instanceof DerivationConfigWithExtractor) {
      DerivationConfigWithExtractor derivationConfigWithExtractor = (DerivationConfigWithExtractor) derivationConfig;
      inputs = derivationConfigWithExtractor.getInputs().stream().map(KeyedFeature::getFeature).
          collect(Collectors.toSet());
    } else if (derivationConfig instanceof SimpleDerivationConfig) {
      SimpleDerivationConfig simpleDerivationConfig = (SimpleDerivationConfig) derivationConfig;
      /*
       * For derived feature defined as SimpleDerivationConfig, we only have the feature expression.
       * The base features in feature expression should be in the set of defined features.
       */
      String featureExpr = simpleDerivationConfig.getFeatureExpr();
      Matcher matcher = FEATURE_NAME_PATTERN.matcher(featureExpr);

      inputs = new HashSet<>();
      while (matcher.find()) {
        String word = matcher.group(1);
        if (allDefinedFeatureNames.contains(word)) {
          inputs.add(word);
        }
      }
    } else if (derivationConfig instanceof SequentialJoinConfig) {
      // for sequential join feature, the input is the base feature and expansion feature
      SequentialJoinConfig sequentialJoinConfig = (SequentialJoinConfig) derivationConfig;
      inputs = Stream.of(sequentialJoinConfig.getBase().getFeature(), sequentialJoinConfig.getExpansion().getFeature())
          .collect(Collectors.toSet());
    } else {
      throw new RuntimeException("The DerivationConfig type of " + derivedFeature + " is not supported.");
    }

    return inputs;
  }
}