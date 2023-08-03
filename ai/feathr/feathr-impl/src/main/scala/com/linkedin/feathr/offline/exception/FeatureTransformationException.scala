package com.linkedin.feathr.offline.exception

/**
 * This exception is thrown when feature transformation fails
 */
private[offline] class FeatureTransformationException(message: String) extends Exception(message) {

  def this(message: String, cause: Throwable) {
    this(message)
    initCause(cause)
  }
}
