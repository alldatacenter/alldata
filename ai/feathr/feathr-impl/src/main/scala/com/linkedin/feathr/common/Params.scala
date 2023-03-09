package com.linkedin.feathr.common

import com.typesafe.config.Config

/**
  * parameter map used in various of classes, such as anchor extractor, derived feature,
  * source key extractor, etc.
  *
  * This helps user to pass arbitrary parameter into the related classes when needed.
  */
trait Params extends Serializable {
  var _params: Option[Config] = None

  /**
    * init with parameter map
    * @param _params
    */
  def init(params: Config) = {
    _params = Some(params)
  }
}