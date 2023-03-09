package com.linkedin.feathr.offline.source.pathutil

import org.apache.spark.sql.SparkSession
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler

/**
 * Common path utility functions
 */
private[offline] trait PathChecker {
  /**
   * check whether the path is a local mock folder
   * @param path input path
   * @return true if the local mock folder exists.
   */
  def isMock(path: String) : Boolean

  /**
   * check whether the input path exists.
   * @param path input path.
   * @return true if the path exists.
   */
  def exists(path: String) : Boolean
}

/**
 * It will construct a specific path checker according to the spark session.
 */
private[offline] object PathChecker {
  def apply(ss : SparkSession, dataLoaderHandlers: List[DataLoaderHandler]): PathChecker = {
    if (ss.sparkContext.isLocal) new LocalPathChecker(ss.sparkContext.hadoopConfiguration, dataLoaderHandlers)
    else new HdfsPathChecker()
  }
}
