package com.linkedin.feathr.offline.source.dataloader

import com.linkedin.feathr.offline.config.location.DataLocation
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler
import org.apache.logging.log4j.LogManager
import org.apache.spark.customized.CustomGenericRowWithSchema
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.hadoop.mapred.JobConf


/**
 * DataLoaderFactory trait that will create a data loader based on the type of the input.
 */
private[offline] trait DataLoaderFactory {
  @transient lazy val log = LogManager.getLogger(getClass.getName)
  /**
   * create a data loader based on the file type.
   *
   * @param path the input file path
   * @return a [[DataLoader]]
   */
  def create(path: String): DataLoader

  def createFromLocation(input: DataLocation): DataLoader = create(input.getPath)
}

private[offline] object DataLoaderFactory {
  CustomGenericRowWithSchema
  /**
   * construct a specific loader factory based on whether the spark session is local or not.
   */
  def apply(ss: SparkSession, streaming: Boolean = false, dataLoaderHandlers: List[DataLoaderHandler]): DataLoaderFactory = {
    if (streaming) {
      new StreamingDataLoaderFactory(ss)
    }
    else if (ss.sparkContext.isLocal) {
      // For test
      new LocalDataLoaderFactory(ss, dataLoaderHandlers)
    } else {
      new BatchDataLoaderFactory(ss, dataLoaderHandlers)
    }
  }
}

/**
 * Class that encloses hooks for creating/writing data frames depends on the data/path type.
 * @param validatePath used to validate if path should be routed to data handler
 * @param createDataFrame  used to create a data feathr given a path.
 * @param createUnionDataFrame used to create a data frame given multiple paths
 * @param writeDataFrame used to write a data frame to a path
 */
case class DataLoaderHandler(
  validatePath: String => Boolean,
  createDataFrame: (String, Map[String, String], JobConf) => DataFrame,
  createUnionDataFrame: (Seq[String], Map[String, String], JobConf) => DataFrame,
  writeDataFrame: (DataFrame, String,  Map[String, String]) => Unit,
)

