package com.linkedin.feathr.offline.mvel.plugins;

import com.linkedin.feathr.common.FeatureValue;

/**
 * A converter that knows how to convert back and forth between Feathr's {@link FeatureValue} and some other
 * representation. Implementations' "to" and "from" methods are expected to be inverses of each other, such that
 * toFeathrFeatureValue(fromFeathrFeatureValue(x)) = x and fromFeathrFeatureValue(toFeathrFeatureValue(x)) = x,
 * for any instance 'x' of FeatureValue.
 *
 * NOTE: This class is intended for advanced users only, and specifically as a "migration aid" for migrating from
 * some previous versions of Feathr whose FeatureValue representations had a different class name, while preserving
 * compatibility with feature definitions written against those older versions of Feathr.
 *
 * @param <T> the other, alternative FeatureValue representation
 */
public interface FeatureValueTypeAdaptor<T> {

  /**
   * Convert from the "other" representation into Feathr FeatureValue.
   *
   * It is expected to be the inverse of {@link #fromFeathrFeatureValue(FeatureValue)}
   *
   * @param other the feature value in the "other" representation
   * @return the feature value represented as a Feathr FeatureValue
   */
  FeatureValue toFeathrFeatureValue(T other);

  /**
   * Convert from Feathr FeatureValue into the "other" representation.
   *
   * It is expected to be the inverse of {@link #toFeathrFeatureValue(Object)}
   *
   * @param featureValue the feature value represented as a Feathr FeatureValue
   * @return the feature value in the "other" representation
   */
  T fromFeathrFeatureValue(FeatureValue featureValue);
}
