package com.linkedin.feathr.common

import com.typesafe.config.Config

/**
  * this is the common trait for derivation function for Feathr internal use
  * if Feathr user need to implement customized derivation, please use [[FeatureDerivationFunction]]
  * or [[FeatureDerivationFunctionSpark]]
*/
abstract class FeatureDerivationFunctionBase extends Serializable {

  var _params: Option[Config] = None

  /**
    * init with parameter map
    * @param _params
    */
  def init(params: Config) = {
    _params = Some(params)
  }

  /**
    * return dependency/input feature list
    * This function is only used in derivation function that produces multiple derived features, i.e., derived features
    * defined within 'advancedDerivations' section. Derived features defined in  'derivations' section do not need to override this.
    */
  def getInputFeatureList(): Seq[JoiningFeatureParams] = throw
    new RuntimeException("Unsupported operation. Please override this method if this is an advanced derivation function.")

  /**
    * return output feature list
    * Used together with getInputFeatureList()
    */
  def getOutputFeatureList(): Seq[String] =  throw
    new RuntimeException("Unsupported operation. Please override this method if this is an advanced derivation function.")
}
