package com.linkedin.feathr.offline.transformation

// FDS representation of auto-tensorized NTV feature
private[offline] case class FDS1dTensor(indices0: Seq[String], values: Seq[Float])
