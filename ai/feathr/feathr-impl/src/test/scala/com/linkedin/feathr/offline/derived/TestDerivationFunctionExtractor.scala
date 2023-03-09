package com.linkedin.feathr.offline.derived

import com.linkedin.feathr.common.{FeatureDerivationFunction, FeatureValue}
// Extractor used in derived feature in TestFeatureJoinJob
// test the functionality of using UDF in derived features
class TestDerivationFunctionExtractor extends FeatureDerivationFunction {
  override def getFeatures(inputs: Seq[Option[FeatureValue]]): Seq[Option[FeatureValue]] = {
    if (inputs == null) {
      Seq(Option(new FeatureValue()))
    } else {
      if (inputs.size != 2) {
        Seq(Option(new FeatureValue()))
      } else {
        val result = (inputs(0), inputs(1)) match {
          case (Some(x), Some(y)) => {
            x.getAsTermVector.putAll(y.getAsTermVector)
            Some(new FeatureValue(x))
          }
          // In any case that we don't have at least one of the term maps, return None
          case _ => {
            Option(new FeatureValue())
          }
        }
        Seq(result)
      }
    }
  }
}
