package com.linkedin.feathr.offline.client.plugins

import com.linkedin.feathr.common.{AnchorExtractor, FeatureDerivationFunction}
import com.linkedin.feathr.sparkcommon.{SimpleAnchorExtractorSpark, SourceKeyExtractor}

/**
 * Tells Feathr how to use UDFs that are defined in "external" non-Feathr UDF classes (i.e. that don't extend from
 * Feathr's AnchorExtractor or other UDF traits). An adaptor must match the external UDF class to a specific kind of
 * Feathr UDF – see child traits below for the various options.
 *
 * All "external" UDF classes are required to have a public default zero-arg constructor.
 *
 * @tparam T the internal Feathr UDF class whose behavior the external UDF can be translated to
 */
sealed trait UdfAdaptor[T] extends Serializable {
  /**
   * Indicates whether this adaptor can be applied to an object of the provided class.
   *
   * Implementations should usually look like <pre>classOf[UdfTraitThatIsNotPartOfFeathr].isAssignableFrom(clazz)</pre>
   *
   * @param clazz some external UDF type
   * @return true if this adaptor can "adapt" the given class type; false otherwise
   */
  def canAdapt(clazz: Class[_]): Boolean

  /**
   * Returns an instance of a Feathr UDF, that follows the behavior of some external UDF instance, e.g. via delegation.
   *
   * @param externalUdf instance of the "external" UDF
   * @return the Feathr UDF
   */
  def adaptUdf(externalUdf: AnyRef): T
}

/**
 * An adaptor that can "tell Feathr how to use" a UDF type that can act in place of [[AnchorExtractor]]
 */
trait AnchorExtractorAdaptor extends UdfAdaptor[AnchorExtractor[_]]

/**
 * An adaptor that can "tell Feathr how to use" a UDF type that can act in place of [[SimpleAnchorExtractorSpark]]
 */
trait SimpleAnchorExtractorSparkAdaptor extends UdfAdaptor[SimpleAnchorExtractorSpark]

/**
 * An adaptor that can "tell Feathr how to use" a UDF type that can act in place of [[FeatureDerivationFunction]]
 */
trait FeatureDerivationFunctionAdaptor extends UdfAdaptor[FeatureDerivationFunction]

/**
 * An adaptor that can "tell Feathr how to use" a UDF type that can act in place of [[SourceKeyExtractor]]
 */
trait SourceKeyExtractorAdaptor extends UdfAdaptor[SourceKeyExtractor]