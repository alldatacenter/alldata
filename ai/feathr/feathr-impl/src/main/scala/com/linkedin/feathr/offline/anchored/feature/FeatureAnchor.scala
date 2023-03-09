package com.linkedin.feathr.offline.anchored.feature

import com.linkedin.feathr.common
import com.linkedin.feathr.common.{AnchorExtractor, FeatureTypeConfig, FeatureTypes}
import com.linkedin.feathr.sparkcommon.SourceKeyExtractor
import com.linkedin.feathr.swj.LateralViewParams

/**
 * An Anchor defines how a feature or set of features can be loaded from the platform.
 *
 * @param sourceIdentifier identifier structure of the data source, defined in the same configuration file, generally a path in HDFS
 * @param extractor transforms the data into Feathr's util feature data schema
 * @param defaults provides default values for the features made available by the transformer
 * @param lateralViewParams lateral view parameter for SWA feature
 * @param sourceKeyExtractor source key extractor, used to extract keys for the source within the anchor
 * @param features features that are defined under this anchor
 * @param featureTypeConfigs FeatureTypeConfig of features in this anchor
 */
private[offline] case class FeatureAnchor(
    sourceIdentifier: String,
    extractor: AnyRef,
    defaults: Map[String, common.FeatureValue],
    lateralViewParams: Option[LateralViewParams],
    sourceKeyExtractor: SourceKeyExtractor,
    features: Set[String],
    featureTypeConfigs: Map[String, FeatureTypeConfig] = Map()) {
  def getProvidedFeatureNames: Seq[String] = {
    features.toSeq
  }

  def getFeatureTypes: Option[Map[String, FeatureTypes]] = {
    Some(featureTypeConfigs map { case (key, value) => (key, value.getFeatureType) })
  }

  /**
    * get the row-based AnchorExtractor, note that some of the derivations
    * are not subclass of AnchorExtractor, e.g, [[SimpleAnchorExtractorSpark]], in such cases, this function will
    * throw exception, make sure you will not call this function for such cases.
    */
  def getAsAnchorExtractor: AnchorExtractor[Any] = extractor.asInstanceOf[AnchorExtractor[Any]]
  override def toString: String =
    "sourceIdentifier:" + sourceIdentifier + ", extractor: " + extractor.toString +
      ", sourceKeyExtractor: " + sourceKeyExtractor.toString()
}
