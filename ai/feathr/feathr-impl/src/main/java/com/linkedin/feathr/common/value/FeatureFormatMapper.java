package com.linkedin.feathr.common.value;

import com.linkedin.feathr.common.types.FeatureType;


/**
 * A mapper, or translator, that can convert between the standard FeatureValue representation and some other
 * external representation.
 * @param <T> type of the external representation
 */
public interface FeatureFormatMapper<T> {

  /**
   * Converts a feature value into the external representation
   * @param featureValue the FeatureValue
   * @return the datum in the external representation
   */
  T fromFeatureValue(FeatureValue featureValue);

  /**
   * Converts a datum in the external representation into the FeatureValue format. The FeatureType of the feature must
   * be known.
   * @param featureType type of this feature
   * @param externalValue datum in the external representation
   * @return datum represented as a FeatureValue
   */
  FeatureValue toFeatureValue(FeatureType featureType, T externalValue);
}
