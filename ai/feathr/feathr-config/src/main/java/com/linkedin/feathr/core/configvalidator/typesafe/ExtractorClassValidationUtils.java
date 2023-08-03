package com.linkedin.feathr.core.configvalidator.typesafe;

import com.linkedin.feathr.core.config.ConfigType;
import com.linkedin.feathr.core.config.consumer.JoinConfig;
import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfig;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfigWithExtractor;
import com.linkedin.feathr.core.config.producer.anchors.AnchorConfigWithKeyExtractor;
import com.linkedin.feathr.core.config.producer.anchors.AnchorsConfig;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfig;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfigWithExtractor;
import com.linkedin.feathr.core.config.producer.derivations.DerivationsConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilder;
import com.linkedin.feathr.core.configdataprovider.ConfigDataProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Utils to validate extractor classes in FeatureDef config, to check if extractor classes are defined in jars
 *
 * This is designed for independent usage of FeatureConsumerConfValidator or FeatureProducerConfValidator,
 * as extractor class validation has different Gradle task dependency from general Frame config validation (performed by
 * FeatureConsumerConfValidator or FeatureProducerConfValidator).
 *
 * For general Frame config validation, the validation need to be performed before jar task.
 * For extractor class validation, the validation need to wait for all jars built, to search if the depended jars
 * contain the definition of the extractor class
 *
 * Since Gradle has more powerful APIs to process jar file. The validation logic (including jar searching)
 * will be placed in Gradle plugins which perform the validation.
 * And instead of building a ExtractorClassValidator class, here we only build some public utils that can be used
 * for extractor class validation.
 */
public class ExtractorClassValidationUtils {

  // Util class
  private ExtractorClassValidationUtils() {

  }

  /**
   * Get a list of full class names of extractors in FeatureDef config for anchors and derivations.
   * If the join config is specified, then only get extractors associated with required features.
   * If the join config is not specified, then get all extractors defined in FeatureDef config.
   *
   * Note classes in MVELs are skipped.
   */
  public static Set<String> getExtractorClasses(Map<ConfigType, ConfigDataProvider> configDataProviderMap) {
    Set<String> allClasses = new HashSet<>();

    ConfigBuilder configBuilder = ConfigBuilder.get();
    if (configDataProviderMap.containsKey(ConfigType.FeatureDef)) {
      FeatureDefConfig featureDefConfig =
          configBuilder.buildFeatureDefConfig(configDataProviderMap.get(ConfigType.FeatureDef));

      // mapping from anchor name to feature name set
      Map<String, Set<String>> anchorFeaturesMap = new HashMap<>();

      /*
       * mapping from anchor name to extractor name list,
       * one anchor can have at most two extractors (extractor and key extractor)
       */
      Map<String, List<String>> anchorExtractorsMap = getExtractorClassesInAnchors(featureDefConfig, anchorFeaturesMap);
      // mapping from derived feature name to extractor name
      Map<String, String> derivedExtractorMap = getExtractorClassesInDerivations(featureDefConfig);

      /*
       * If the join config is specified, then only get extractors associated with required features.
       * else get all extractors defined in FeatureDef config.
       */
      if (configDataProviderMap.containsKey(ConfigType.Join)) {
        JoinConfig joinConfig = configBuilder.buildJoinConfig(configDataProviderMap.get(ConfigType.Join));
        Set<String> requiredFeatureNames = FeatureDefConfigSemanticValidator.getRequiredFeatureNames(featureDefConfig,
            JoinConfSemanticValidator.getRequestedFeatureNames(joinConfig));

        return filterClassesWithRequiredFeatures(requiredFeatureNames, anchorExtractorsMap, anchorFeaturesMap,
            derivedExtractorMap);
      } else {
        allClasses.addAll(anchorExtractorsMap.values().stream().flatMap(List::stream).collect(Collectors.toSet()));
        allClasses.addAll(derivedExtractorMap.values());
      }
    } // else no op if there is no FeatureDef config, and empty set will be returned

    return allClasses;
  }

  /**
   * Given a {@link FeatureDefConfig} object, get mapping from anchor name to extractor name list,
   * one anchor can have at most two extractors (extractor and key extractor)
   * @param featureDefConfig the {@link FeatureDefConfig} object
   * @param anchorFeaturesMap the container map, that maps anchor name to the set of features. The information can
   *                          lately be used to have a mapping from anchored feature name to extractor name.
   *                          The mapping from feature name to extractor name contains a lot of
   *                          redundant information as multiple features with the same
   *                          anchor can share the same extractor. Also, this information is optional for later
   *                          processing.
   * @return mapping from anchor name to extractor name list.
   */
  private static Map<String, List<String>> getExtractorClassesInAnchors(FeatureDefConfig featureDefConfig,
      Map<String, Set<String>> anchorFeaturesMap) {
    Map<String, List<String>> anchorExtractorsMap = new HashMap<>();

    Map<String, AnchorConfig> anchors = featureDefConfig.getAnchorsConfig()
        .orElse(new AnchorsConfig(new HashMap<>())).getAnchors();

    for (Map.Entry<String, AnchorConfig> entry: anchors.entrySet()) {
      String anchorName = entry.getKey();
      AnchorConfig anchor = entry.getValue();
      if (anchor instanceof AnchorConfigWithExtractor) {
        AnchorConfigWithExtractor anchorWithExtractor = (AnchorConfigWithExtractor) anchor;
        // collect extractors, might be two (extractor and keyExtractor)
        anchorExtractorsMap.put(anchorName, new ArrayList<>(Arrays.asList(anchorWithExtractor.getExtractor())));
        anchorWithExtractor.getKeyExtractor().map(e -> anchorExtractorsMap.get(anchorName).add(e));
        // collect features
        anchorFeaturesMap.put(anchorName, anchorWithExtractor.getFeatures().keySet());
      } else if (anchor instanceof AnchorConfigWithKeyExtractor) {
        AnchorConfigWithKeyExtractor anchorWithKeyExtractor = (AnchorConfigWithKeyExtractor) anchor;
        anchorExtractorsMap.put(anchorName, Collections.singletonList(anchorWithKeyExtractor.getKeyExtractor()));
        anchorFeaturesMap.put(anchorName, anchorWithKeyExtractor.getFeatures().keySet());
      }
    }
    return anchorExtractorsMap;
  }

  /**
   * Given a {@link FeatureDefConfig} object, get mapping from derived feature name to extractor class name
   */
  private static Map<String, String> getExtractorClassesInDerivations(FeatureDefConfig featureDefConfig) {
    Map<String, DerivationConfig> derivations = featureDefConfig.getDerivationsConfig()
        .orElse(new DerivationsConfig(new HashMap<>())).getDerivations();
    // mapping from derived feature  to the extractor used
    Map<String, String> derivedExtractorMap = new HashMap<>();

    for (Map.Entry<String, DerivationConfig> entry: derivations.entrySet()) {
      String derivedFeature = entry.getKey();
      DerivationConfig derivation = entry.getValue();
      if (derivation instanceof DerivationConfigWithExtractor) {
        DerivationConfigWithExtractor derivationWithExtractor = (DerivationConfigWithExtractor) derivation;
        derivedExtractorMap.put(derivedFeature, derivationWithExtractor.getClassName());
      }
      /*
       * Here skip classes in MVEL expressions. In some derivations, such as online derivations sometime the MVEL
       * expression can import some classes with "import", or the optional transformation expression used in
       * sequential join.
       */
    }
    return derivedExtractorMap;
  }

  /**
   * Get all extractor classes associated with required features
   * @param requiredFeatureNames required feature names
   * @param anchorExtractorsMap mapping from anchor name to extractor class names
   * @param anchorFeaturesMap mapping from anchor name to feature name
   * @param derivedExtractorMap mapping from derived feature name to extractor class name
   * @return all extractor classes associated with required features
   */
  private static Set<String> filterClassesWithRequiredFeatures(Set<String> requiredFeatureNames,
      Map<String, List<String>> anchorExtractorsMap, Map<String, Set<String>> anchorFeaturesMap,
      Map<String, String> derivedExtractorMap) {
    Set<String> allClasses = new HashSet<>();

    // get required anchors, whose features are required
    Set<String> requiredAnchors = anchorFeaturesMap.entrySet().stream()
        .filter(e -> e.getValue().removeAll(requiredFeatureNames)) // check if at least one feature in anchor is required
        .map(Map.Entry::getKey).collect(Collectors.toSet());

    // collect extractor classes whose anchors are required
    anchorExtractorsMap.entrySet().stream()
        .filter(e -> requiredAnchors.contains(e.getKey())).map(Map.Entry::getValue)
        .forEach(allClasses::addAll);

    // collect extractor class of derived features that are required
    derivedExtractorMap.entrySet().stream().filter(e -> requiredFeatureNames.contains(e.getKey()))
        .map(Map.Entry::getValue)
        .forEach(allClasses::add);

    return allClasses;
  }
}