package com.linkedin.feathr.offline.plugins;

import com.linkedin.feathr.common.FeatureValue;
import com.linkedin.feathr.common.types.NumericFeatureType;
import com.linkedin.feathr.offline.mvel.plugins.FeatureValueTypeAdaptor;

import java.io.Serializable;

public class AlienFeatureValueTypeAdaptor implements FeatureValueTypeAdaptor<AlienFeatureValue>, Serializable {
  @Override
  public FeatureValue toFeathrFeatureValue(AlienFeatureValue other) {
    if (other.isFloat()) {
      return FeatureValue.createNumeric(other.getFloatValue());
    } else if (other.isString()) {
      return FeatureValue.createCategorical(other.getStringValue());
    } else {
      throw new RuntimeException();
    }
  }

  @Override
  public AlienFeatureValue fromFeathrFeatureValue(FeatureValue featureValue) {
    if (featureValue.getFeatureType().equals(NumericFeatureType.INSTANCE)) {
      return AlienFeatureValue.fromFloat(featureValue.getAsNumeric());
    } else {
      // Unclear whether old FeatureValue API makes it very easy to determine whether feature is actual a CATEGORICAL,
      // so for purposes of testing the plugin API, don't worry about it and just assume.
      return AlienFeatureValue.fromString(featureValue.getAsCategorical());
    }
  }
}
