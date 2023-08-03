package com.linkedin.feathr.common.value;

import com.linkedin.feathr.common.Experimental;
import com.linkedin.feathr.common.types.FeatureType;


/**
 * Represents a value of a feature in Feathr.
 */
public interface FeatureValue {

  /**
   * @return the {@link FeatureType} of this feature
   */
  FeatureType getFeatureType();
}
