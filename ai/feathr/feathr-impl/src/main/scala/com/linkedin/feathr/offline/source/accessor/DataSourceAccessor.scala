package com.linkedin.feathr.offline.source.accessor

import com.linkedin.feathr.offline.source.dataloader.DataLoaderFactory
import com.linkedin.feathr.offline.source.pathutil.{PathChecker, TimeBasedHdfsPathAnalyzer}
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler
import com.linkedin.feathr.offline.source.{DataSource, SourceFormatType}
import com.linkedin.feathr.offline.util.PartitionLimiter
import com.linkedin.feathr.offline.util.datetime.DateTimeInterval
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.io.File

/**
 * the base class to access and load the data source
 * The DataSourceAccessor can load a fixed path, a list of path as well as a path containing time series data.
 * @param source the data source.
 */
private[offline] abstract class DataSourceAccessor(val source: DataSource) {

  /**
   * get source data as a dataframe
   *
   * @return the dataframe
   */
  def get(): DataFrame
}

private[offline] object DataSourceAccessor {

  /**
   * create time series/composite source that contains multiple day/hour data
   *
   * @param ss              spark session
   * @param source          source definition
   * @param dateIntervalOpt timespan of dataset
   * @param expectDatumType expect datum type in RDD form
   * @param failOnMissingPartition   whether to fail the file loading if some of the date partitions are missing.
   * @param addTimestampColumn whether to create a timestamp column from the time partition of the source.
   * @param isStreaming whether data source is streaming
   * @param dataPathHandlers hooks for users to add their own DataSourceAccessor and DataLoaderFactory
   * @return a TimeSeriesSource
   */
  def apply(
      ss: SparkSession,
      source: DataSource,
      dateIntervalOpt: Option[DateTimeInterval],
      expectDatumType: Option[Class[_]],
      failOnMissingPartition: Boolean,
      addTimestampColumn: Boolean = false,
      isStreaming: Boolean = false,
      dataPathHandlers: List[DataPathHandler]): DataSourceAccessor = { //TODO: Add tests

    val dataAccessorHandlers: List[DataAccessorHandler] = dataPathHandlers.map(_.dataAccessorHandler)
    val dataLoaderHandlers: List[DataLoaderHandler] = dataPathHandlers.map(_.dataLoaderHandler)

    val sourceType = source.sourceType
    val dataLoaderFactory = DataLoaderFactory(ss, isStreaming, dataLoaderHandlers)
    if (isStreaming) {
      new StreamDataSourceAccessor(ss, dataLoaderFactory, source)
    } else if (dateIntervalOpt.isEmpty || sourceType == SourceFormatType.FIXED_PATH || sourceType == SourceFormatType.LIST_PATH) {
      // if no input interval, or the path is fixed or list, load whole dataset
      new NonTimeBasedDataSourceAccessor(ss, dataLoaderFactory, source, expectDatumType)
    } else {
      import scala.util.control.Breaks._
      
      val timeInterval = dateIntervalOpt.get
      var dataAccessorOpt: Option[DataSourceAccessor] = None
      breakable {
        for (dataAccessorHandler <- dataAccessorHandlers) {
          if (dataAccessorHandler.validatePath(source.path)) {
            dataAccessorOpt = Some(dataAccessorHandler.getAccessor(ss, source, timeInterval, expectDatumType, failOnMissingPartition, addTimestampColumn))
            break
          }
        }
      }
      val dataAccessor = dataAccessorOpt match {
        case Some(dataAccessor) => dataAccessor
        case _ => createFromHdfsPath(ss, source, timeInterval, expectDatumType, failOnMissingPartition, addTimestampColumn, dataLoaderHandlers)
      }
      dataAccessor
    }
  }

  /**
   * create DataSourceAccessor from HDFS path
   *
   * @param ss              spark session
   * @param source          source definition
   * @param timeInterval timespan of dataset
   * @param expectDatumType expect datum type in RDD form
   * @param failOnMissingPartition whether to fail the file loading if some of the date partitions are missing.
   * @param addTimestampColumn whether to create a timestamp column from the time partition of the source.
   * @param dataLoaderHandlers a hook for users to add their own DataLoaderFactory
   * @return a TimeSeriesSource
   */
  private def createFromHdfsPath(
      ss: SparkSession,
      source: DataSource,
      timeInterval: DateTimeInterval,
      expectDatumType: Option[Class[_]],
      failOnMissingPartition: Boolean,
      addTimestampColumn: Boolean,
      dataLoaderHandlers: List[DataLoaderHandler]): DataSourceAccessor = {
    val pathChecker = PathChecker(ss, dataLoaderHandlers)
    val fileLoaderFactory = DataLoaderFactory(ss = ss, dataLoaderHandlers = dataLoaderHandlers)
    val partitionLimiter = new PartitionLimiter(ss)
    val pathAnalyzer = new TimeBasedHdfsPathAnalyzer(pathChecker, dataLoaderHandlers)
    val fileName = new File(source.path).getName
    if (source.timePartitionPattern.isDefined) {
      // case 1: the timePartitionPattern exists
      val pathInfo = pathAnalyzer.analyze(source.path, source.timePartitionPattern.get)
      PathPartitionedTimeSeriesSourceAccessor(
        ss,
        pathChecker,
        fileLoaderFactory,
        partitionLimiter,
        pathInfo,
        source,
        timeInterval,
        failOnMissingPartition,
        addTimestampColumn)
    } else {
      // legacy configurations without timePartitionPattern
      if (fileName.endsWith("daily") || fileName.endsWith("hourly") || source.sourceType == SourceFormatType.TIME_PATH) {
        // case 2: if it's daily/hourly data, load the partitions
        val pathInfo = pathAnalyzer.analyze(source.path)
        PathPartitionedTimeSeriesSourceAccessor(
          ss,
          pathChecker,
          fileLoaderFactory,
          partitionLimiter,
          pathInfo,
          source,
          timeInterval,
          failOnMissingPartition,
          addTimestampColumn)
      } else {
        // case 3: load as whole dataset
        new NonTimeBasedDataSourceAccessor(ss, fileLoaderFactory, source, expectDatumType)
      }
    }
  }
}

/**
 * Class that contains hooks for getting and accessors for a validated path
 * @param validatePath path validator hook.
 * @param getAccessor accessor getter hook.
 */
private[offline] case class DataAccessorHandler(
  validatePath: String => Boolean,
  getAccessor: 
  (
    SparkSession,
    DataSource,
    DateTimeInterval,
    Option[Class[_]],
    Boolean,
    Boolean
  ) => DataSourceAccessor
)

/**
 * Class that encapsulates the accessor handling and data loader handling for a given file path type
 * @param dataAccessorHandler the handler for data accessors.
 * @param dataLoaderHandler the handler for data loaders.
 */
private[offline] case class DataPathHandler(
  dataAccessorHandler: DataAccessorHandler,
  dataLoaderHandler: DataLoaderHandler
)