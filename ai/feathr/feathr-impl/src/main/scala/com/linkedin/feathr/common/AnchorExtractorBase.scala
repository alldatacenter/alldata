package com.linkedin.feathr.common

import com.typesafe.config.Config

/**
  * base class for feature extractor class,
  * e.g., AnchorExtractor, SimpleAnchorExtractorSpark and GenericAnchorExtractorSpark
  * @tparam T row data type
  */
trait AnchorExtractorBase[T] extends Serializable {
  var _params: Option[Config] = None

  /**
    * init with parameter map
    * @param _params
    */
  def init(params: Config) = {
    _params = Some(params)
  }

  /**
    * The names of the features provided by this anchor.
    */
  def getProvidedFeatureNames: Seq[String]
}