package com.linkedin.feathr.offline.derived

import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.common.{ErasedEntityTaggedFeature, FeatureDerivationFunction, FeatureTypeConfig, FeatureTypes}
import com.linkedin.feathr.sparkcommon.FeatureDerivationFunctionSpark

/**
 * Internal class for derived feature
 *
 * @param consumedFeatureNames list of dependency feature names, ordered by the map key when loading config as a map
 * @param producedFeatureNames features produced by this derivedFeature
 * @param derivation derivation function
 * @param parameterNames  parameterNames MUST BE in same order as consumedFeatureNames, in the above example,
 *                        C does not have parameterNames
 *                        E has parameterNames as seq['nc', 'nd'], e.g, the keys in the 'inputs' object
 *                        F has no parameterNames
 */
private[offline] case class DerivedFeature(
    consumedFeatureNames: Seq[ErasedEntityTaggedFeature],
    producedFeatureNames: Seq[String],
    derivation: AnyRef,
    parameterNames: Option[Seq[String]] = None,
    featureTypeConfigs: Map[String, FeatureTypeConfig] = Map()) {

  def getFeatureTypes: Map[String, FeatureTypes] = {
    featureTypeConfigs map { case (key, value) => (key, value.getFeatureType) }
  }

  /**
   * get the row-based FeatureDerivationFunction, note that some of the derivations that derive from FeatureDerivationFunctionBase
   * are not subclass of FeatureDerivationFunction, e.g, [[FeatureDerivationFunctionSpark]], in such cases, this function will
   * throw exception, make sure you will not call this function for such cases.
   *
   * TODO: The above described condition is bad; ideally this class should capture the information about what type of
   *       derivation function this is in a type-safe way.
   */
  def getAsFeatureDerivationFunction(): FeatureDerivationFunction = derivation.asInstanceOf[FeatureDerivationFunction]

  // get output feature list for different types of derivation functions
  def getOutputFeatureList(): Seq[String] = derivation match {
    case derived: FeatureDerivationFunction => getAsFeatureDerivationFunction.getOutputFeatureList()
    case derived: FeatureDerivationFunctionSpark => derived.getOutputFeatureList()
    case derived =>
      throw new FeathrConfigException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"Unsupported derivation class ${derived}. Only FeatureDerivationFunction and FeatureDerivationFunctionSpark are supported.")
  }
  val dependencyFeatures: Set[String] = consumedFeatureNames.map(_.getFeatureName).toSet
}
