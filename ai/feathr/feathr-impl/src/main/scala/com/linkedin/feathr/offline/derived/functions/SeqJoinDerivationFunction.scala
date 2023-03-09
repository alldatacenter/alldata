package com.linkedin.feathr.offline.derived.functions

import com.linkedin.feathr.common
import com.linkedin.feathr.common.FeatureDerivationFunction
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException}

import com.linkedin.feathr.offline.config.{BaseTaggedDependency, TaggedDependency}

/**
 * A derivation function representing a sequential join feature. A sequential join feature
 * is a feature obtained by LEFT OUTER join the left-side feature's value with the right-side feature's key.
 *
 * Note: This class serves as a data container object rather than holding any computation logic.
 * Sequential join computation will be done in FeathrClient directly. As a result, the `getFeatures`
 * method is a dummy method and shouldn't be used.
 */
private[offline] class SeqJoinDerivationFunction(val left: BaseTaggedDependency, val right: TaggedDependency, val aggregation: String)
  extends FeatureDerivationFunction {

  /**
   * WARNING: Dummy getFeatures method. This method shouldn't be called as SeqJoin is not computed here.
   */
  override def getFeatures(inputs: Seq[Option[common.FeatureValue]]): Seq[Option[common.FeatureValue]] = {
    throw new FeathrException(ErrorLabel.FEATHR_ERROR, "getFeatures() does not apply to SeqJoinDerivationFunction")
  }
}
