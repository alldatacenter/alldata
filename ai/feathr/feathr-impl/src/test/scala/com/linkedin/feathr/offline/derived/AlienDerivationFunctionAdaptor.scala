package com.linkedin.feathr.offline.derived

import com.linkedin.feathr.common.{FeatureDerivationFunction, FeatureValue}
import com.linkedin.feathr.offline.client.plugins.FeatureDerivationFunctionAdaptor
import com.linkedin.feathr.offline.plugins.AlienFeatureValue

class AlienDerivationFunctionAdaptor extends FeatureDerivationFunctionAdaptor {
  /**
   * Indicates whether this adaptor can be applied to an object of the provided class.
   *
   * Implementations should usually look like <pre>classOf[UdfTraitThatIsNotPartOfFeathr].isAssignableFrom(clazz)</pre>
   *
   * @param clazz some external UDF type
   * @return true if this adaptor can "adapt" the given class type; false otherwise
   */
  override def canAdapt(clazz: Class[_]): Boolean = classOf[AlienFeatureDerivationFunction].isAssignableFrom(clazz)

  /**
   * Returns an instance of a Feathr UDF, that follows the behavior of some external UDF instance, e.g. via delegation.
   *
   * @param externalUdf instance of the "external" UDF
   * @return the Feathr UDF
   */
  override def adaptUdf(externalUdf: AnyRef): FeatureDerivationFunction =
    new AlienFeatureDerivationFunctionWrapper(externalUdf.asInstanceOf[AlienFeatureDerivationFunction])

  /**
   * Wrap Alien FeatureDerivationFunction as Feathr FeatureDerivationFunction
   */
  private[derived] class AlienFeatureDerivationFunctionWrapper(derived: AlienFeatureDerivationFunction) extends FeatureDerivationFunction {
    override def getFeatures(inputs: Seq[Option[FeatureValue]]): Seq[Option[FeatureValue]] = {
      derived.getFeatures(Seq(Some(AlienFeatureValue.fromFloat(1.0f))))
      Seq(Some(FeatureValue.createNumeric(1.0f)))
    }
  }
}