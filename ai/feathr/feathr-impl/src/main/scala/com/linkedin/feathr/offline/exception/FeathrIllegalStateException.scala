package com.linkedin.feathr.offline.exception

private[offline] class FeathrIllegalStateException(message: String) extends Exception(message) {

  def this(message: String, cause: Throwable) {
    this(message)
    initCause(cause)
  }
}
