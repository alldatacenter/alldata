package com.linkedin.feathr.offline.exception

/**
 * This exception is thrown when operation is not supported in DataFrame API (vs RDD api)
 * It will be caught in local running mode, and just logging warning message.
 */
private[offline] class DataFrameApiUnsupportedOperationException(message: String) extends Exception(message) {

  def this(message: String, cause: Throwable) {
    this(message)
    initCause(cause)
  }
}
