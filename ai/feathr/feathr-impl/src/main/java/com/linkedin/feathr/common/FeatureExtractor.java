package com.linkedin.feathr.common;

import java.util.Collections;
import java.util.Map;
import java.util.Set;


/**
 * A feature extractor that extracts {@link FeatureValue} from source data.
 * The class must have 0-argument constructor so Feathr engine can instantiate the class with 0-argument constructor.
 * We expect this to replace {@link Extractor} eventually.
 *
 * WARNING: This interface is in beta testing and is not approved for public use yet.
 */
@Experimental
public interface FeatureExtractor<SOURCE_DATA> {
  /**
   * Extract a single feature specified by {@code featureName} from {@code sourceData}.
   * If you only extract a single feature a time and it can be more performant than {@link #batchExtract}, you should
   * implement this.
   * @param featureName Feature name in the Feathr config.
   * @param sourceData The source data from your Feathr Source.
   * @return The {@link FeatureValue} associated with the requested feature name.  When the requested feature cannot
   * be extracted, {@code null} may be returned.
   */
  default FeatureValue extract(String featureName, SOURCE_DATA sourceData) {
    // Provide a default implementation that delegates to getFeatures
    return batchExtract(Collections.singleton(featureName), sourceData).get(featureName);
  }

  /**
   * Extract the set of features specified by {@code featuresToExtract} from the {@code sourceData}.
   * @param featuresToExtract A set of features that this extractor will extract.
   * @param sourceData The source data from your Feathr Source.
   * @return A map of feature name to {@link FeatureValue}. For requested features that cannot be extracted,
   * the expectation is that the result map will not contain entries of those features.
   */
  Map<String, FeatureValue> batchExtract(Set<String> featuresToExtract, SOURCE_DATA sourceData);
}
