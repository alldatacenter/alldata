package com.linkedin.feathr.common


/**
  * Produces a derived feature given some input features
  */
trait FeatureDerivationFunction extends FeatureDerivationFunctionBase {

  def getFeatures(inputs: Seq[Option[FeatureValue]]): Seq[Option[FeatureValue]]

}