package com.linkedin.feathr.offline.source.dataloader

import com.linkedin.feathr.offline.config.location.SimplePath
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler
import org.apache.spark.sql.SparkSession

/**
 * A data loader factory for production HDFS environment. In this case, it will always create a [[BatchDataLoader]].
 *
 * @param ss the spark session.
 */
private[offline] class BatchDataLoaderFactory(ss: SparkSession, dataLoaderHandlers: List[DataLoaderHandler]) extends  DataLoaderFactory {

  /**
   * create a data loader based on the file type.
   *
   * @param path the input file path
   * @return a [[DataLoader]]
   */
  override def create(path: String): DataLoader = {
    log.info(s"Creating spark data loader for path: ${path}")
    new BatchDataLoader(ss, SimplePath(path), dataLoaderHandlers)
  }
}
