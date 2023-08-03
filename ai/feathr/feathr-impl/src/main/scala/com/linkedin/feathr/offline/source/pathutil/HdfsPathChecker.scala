package com.linkedin.feathr.offline.source.pathutil

import com.linkedin.feathr.offline.util.HdfsUtils

/**
 * path checker for non-testing environment.
 */
private[offline] class HdfsPathChecker extends PathChecker {
  override def isMock(path: String): Boolean = false

  override def exists(path: String): Boolean = HdfsUtils.exists(path)
}
