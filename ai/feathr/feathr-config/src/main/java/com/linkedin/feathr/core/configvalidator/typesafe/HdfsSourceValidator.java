package com.linkedin.feathr.core.configvalidator.typesafe;

import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.config.producer.anchors.AnchorsConfig;
import com.linkedin.feathr.core.config.producer.sources.HdfsConfig;
import com.linkedin.feathr.core.config.producer.sources.SourceType;
import com.linkedin.feathr.core.config.producer.sources.SourcesConfig;
import com.linkedin.feathr.core.configvalidator.ValidationResult;
import com.linkedin.feathr.core.configvalidator.ValidationStatus;
import com.linkedin.feathr.core.configvalidator.ValidationType;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * class to validate HDFS resource
 */
class HdfsSourceValidator {

  private static final HdfsSourceValidator HDFS_SOURCE_VALIDATOR = new HdfsSourceValidator();
  private HdfsSourceValidator() {

  }

  static HdfsSourceValidator getInstance() {
    return HDFS_SOURCE_VALIDATOR;
  }
  /*
   * Based on go/dalipolicy, All datasets located under the following directories are managed datasets and should use DALI
   *
   * Note, the policy might be changed, and there is no way to keep it sync.
   * So here we only generate warnings if the user is using managed datasets directly.
   */
  static Set<String> gridManagedDataSets = Stream.of(
      "/data/tracking",
      "/data/tracking_column",
      "/data/databases",
      "/data/service",
      "/data/service_column",
      "/jobs/metrics/ump_v2/metrics",
      "/jobs/metrics/ump_v2/metrics_union",
      "/jobs/metrics/ump_v2/metrics_union_column",
      "/jobs/metrics/udp/snapshot",
      "/jobs/metrics/udp/datafiles").collect(Collectors.toSet());

  /**
   * validate HDFS source in FeatureDef config
   * @param featureDefConfig the {@link FeatureDefConfig} object
   * @return validation result in the format of {@link ValidationResult}
   */
  ValidationResult validate(FeatureDefConfig featureDefConfig) {

    Map<String, String> invalidPaths = getInvalidManagedDataSets(featureDefConfig);
    if (!invalidPaths.isEmpty()) {
      Set<String> invalidSourceInfoSet = invalidPaths.entrySet().stream()
          .map(e -> String.join(": ", e.getKey(), e.getValue()))
          .collect(Collectors.toSet());
      String warnMsg = String.join("", "Based on go/dalipolicy, the following HDFS sources are invalid. ",
          "For managed datasets, you need to use DALI path instead of directly using HDFS path: \n",
          String.join("\n", invalidSourceInfoSet),
          "\nFor detailed information, please refer to go/dalipolicy");
      return new ValidationResult(ValidationType.SEMANTIC, ValidationStatus.WARN, warnMsg);
    }
    return new ValidationResult(ValidationType.SEMANTIC, ValidationStatus.VALID);
  }

  Map<String, String> getInvalidManagedDataSets(FeatureDefConfig featureDefConfig) {
    // first search all source definitions
    Map<String, String> invalidDataSets = featureDefConfig.getSourcesConfig()
        .orElse(new SourcesConfig(Collections.emptyMap())) // return empty map if no sources section
        .getSources().entrySet().stream()
        .filter(e -> e.getValue().getSourceType().equals(SourceType.HDFS)) // get all sources with HDFS
        // get mapping from source name to HDFS path string
        .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), ((HdfsConfig) e.getValue()).getPath()))
        // get all HDFS path with prefix in gridManagedDataSets
        .filter(e -> gridManagedDataSets.stream().anyMatch(prefix -> e.getValue().startsWith(prefix))) // filter invalid
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // then search anchor definitions
    featureDefConfig.getAnchorsConfig()
        .orElse(new AnchorsConfig(Collections.emptyMap()))
        .getAnchors().entrySet().stream()
        .filter(e -> e.getValue().getSource().startsWith("/")) // get all sources with simple HDFS
        // get mapping from anchor name to source path
        .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().getSource()))
        .filter(e -> gridManagedDataSets.stream().anyMatch(prefix -> e.getValue().startsWith(prefix))) // filter invalid
        .forEach(e -> invalidDataSets.put(e.getKey(), e.getValue())); // add to result

    return invalidDataSets;
  }
}


