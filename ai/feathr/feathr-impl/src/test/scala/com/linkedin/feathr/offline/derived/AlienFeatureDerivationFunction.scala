package com.linkedin.feathr.offline.derived

import com.linkedin.feathr.offline.plugins.AlienFeatureValue
/*
 * Sample Alien FeatureDerivationFunction interface that can be adapted into Feathr FeatureDerivationFunction
 */
abstract class AlienFeatureDerivationFunction extends Serializable {
  def getFeatures(inputs: Seq[Option[AlienFeatureValue]]): Seq[Option[AlienFeatureValue]]
}
