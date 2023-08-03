package com.linkedin.feathr.offline.derived
import com.linkedin.feathr.offline.plugins.AlienFeatureValue

class SampleAlienFeatureDerivationFunction extends AlienFeatureDerivationFunction {
  override def getFeatures(inputs: Seq[Option[AlienFeatureValue]]): Seq[Option[AlienFeatureValue]] =
    Seq(Some(AlienFeatureValue.fromFloat(1.0f)))
}
