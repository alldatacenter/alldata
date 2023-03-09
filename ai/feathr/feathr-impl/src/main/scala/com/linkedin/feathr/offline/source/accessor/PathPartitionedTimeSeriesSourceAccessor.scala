package com.linkedin.feathr.offline.source.accessor

import com.linkedin.feathr.common.DateTimeResolution.DateTimeResolution
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrInputDataException}
import com.linkedin.feathr.offline.source.DataSource
import com.linkedin.feathr.offline.source.dataloader.DataLoaderFactory
import com.linkedin.feathr.offline.source.pathutil.{PathChecker, PathInfo, TimeBasedHdfsPathGenerator}
import com.linkedin.feathr.offline.swa.SlidingWindowFeatureUtils
import com.linkedin.feathr.offline.transformation.DataFrameExt._
import com.linkedin.feathr.offline.util.PartitionLimiter
import com.linkedin.feathr.offline.util.SourceUtils.processSanityCheckMode
import com.linkedin.feathr.offline.util.datetime.{DateTimeInterval, OfflineDateTimeUtils}
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.functions.lit
import org.apache.spark.sql.{DataFrame, SparkSession}
/**
 * representation of a source that is comprised of a time series of datasets
 * the idea is that we can call getSourceData with different time window parameter to get a slice of a time series dataset,
 * which contains a dataset for each day/hour in a time window. This would improve the performance significantly,
 * because it could avoid lot of duplicate IOs, e.g., call createDataFrame(/path/to/data) n times in code would
 * result in n times of IOs, but if we wrap this dataset with a class and get the dataset via a method of the class,
 * no matter how many times we call the method, only one IOs/DataFrame object will be needed(if cached).
 * This wrapper also enable Spark to do global catalyst plan optimizations.
 * The class will be used by time aware join and incremental aggregation
 *
 * @param datePartitions seq of each day/hour dataset
 * @param source         source info of the whole dataset
 * @param sourceTimeInterval timespan of dataset
 * @param dateTimeResolution he resolution of the timespan. Either daily or hourly.
 * @param failOnMissingPartition if true, we will validate all the date partitions specified in the inputDateParam exist in the source
 * @param addTimestampColumn whether to create a timestamp column from the time partition of the source.
 * @param partitionLimiter the partition limiter
 */
private[offline] class PathPartitionedTimeSeriesSourceAccessor(
    ss: SparkSession,
    datePartitions: Seq[DatePartition],
    source: DataSource,
    sourceTimeInterval: DateTimeInterval,
    dateTimeResolution: DateTimeResolution,
    failOnMissingPartition: Boolean,
    addTimestampColumn: Boolean,
    partitionLimiter: PartitionLimiter)
    extends TimeBasedDataSourceAccessor(source, sourceTimeInterval, failOnMissingPartition) {

  /**
   * get source data for a particular timespan (subset of the total time window defined)
   *
   * @param timeIntervalOpt timespan of the request source view, only support daily resolution so far
   * @return source view in dataframe formats, defined by input time span,
   *         if timespan is unspecified, return union datasets
   */
  override def get(timeIntervalOpt: Option[DateTimeInterval]): DataFrame = {
    val selectedDatePartitions = getDatePartitionWithinInterval(timeIntervalOpt)
    if (timeIntervalOpt.isDefined && failOnMissingPartition) validateDatePartition(selectedDatePartitions, timeIntervalOpt.get)
    val dataFrames = if (addTimestampColumn) {
      PathPartitionedTimeSeriesSourceAccessor.log.info(s"added timestamp column to source ${source.path}.")
      selectedDatePartitions.map(partition =>
        partition.df.withColumn(SlidingWindowFeatureUtils.TIMESTAMP_PARTITION_COLUMN, lit(partition.dateInterval.getStart.toEpochSecond)))
    } else {
      selectedDatePartitions.map(_.df)
    }
    val df = dataFrames.reduce((x, y) => x.fuzzyUnion(y))

    processSanityCheckMode(ss, partitionLimiter.limitPartition(df))
  }

  /**
   * Get the DatePartitions that fall in the given time interval.
   * If the timeInterval is None, return all the partitions.
   * @param timeIntervalOpt timespan of the request source view
   * @return sequence of DatePartitions that fall in the given time interval.
   */
  def getDatePartitionWithinInterval(timeIntervalOpt: Option[DateTimeInterval]): Seq[DatePartition] = {
    val selectedPartitions = timeIntervalOpt match {
      case Some(interval) =>
        datePartitions.filter(_.dateInterval.overlaps(interval))
      case None =>
        datePartitions
    }
    if (selectedPartitions.isEmpty) {
      throw new FeathrInputDataException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"Trying to create TimeSeriesSource but no data " +
          s"is found to create source data. Source path: ${source.path}, source type: ${source.sourceType}")
    }
    selectedPartitions
  }

  override def overlapWithInterval(interval: DateTimeInterval): Boolean = {
    datePartitions.exists(_.dateInterval.overlaps(interval))
  }

  /**
   * Check if all the date partitions exist in the data source.
   * @param selectedDatePartitions the date partitions available in the source
   * @param timeInterval the request time interval
   */
  private def validateDatePartition(selectedDatePartitions: Seq[DatePartition], timeInterval: DateTimeInterval): Unit = {
    val allDates = timeInterval.getAllTimeWithinInterval(dateTimeResolution).toSet
    val availableDates = selectedDatePartitions.map(_.dateInterval.getStart)
    val missingDates = allDates -- availableDates
    if (missingDates.nonEmpty) {
      val missingDateStr = missingDates.mkString(",")
      throw new FeathrInputDataException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"The following date partitions ${missingDateStr} are missing from the source data. " +
          s"Source path: ${source.path}, source type: ${source.sourceType}")
    }
  }
}

private[offline] object PathPartitionedTimeSeriesSourceAccessor {

  private val log = LogManager.getLogger(getClass)

  /**
   * create time series/composite source that contains multiple day/hour data from a file URI.
   *
   * @param pathChecker the pathChecker
   * @param fileLoaderFactory loader for a single file
   * @param partitionLimiter the partition limiter
   * @param pathInfo analyzed pathInfo
   * @param source source info of the whole dataset
   * @param timeInterval timespan of dataset
   * @param failOnMissingPartition whether to fail the file loading if some of the date partitions are missing.
   * @param addTimestampColumn whether to create a timestamp column from the time partition of the source.
   * @return a TimeSeriesSource
   */
  def apply(
      ss: SparkSession,
      pathChecker: PathChecker,
      fileLoaderFactory: DataLoaderFactory,
      partitionLimiter: PartitionLimiter,
      pathInfo: PathInfo,
      source: DataSource,
      timeInterval: DateTimeInterval,
      failOnMissingPartition: Boolean,
      addTimestampColumn: Boolean): DataSourceAccessor = {
    val pathGenerator = new TimeBasedHdfsPathGenerator(pathChecker)
    val dateTimeResolution = pathInfo.dateTimeResolution
    val postPath = source.postPath
    val postfixPath = if(postPath.isEmpty || postPath.startsWith("/")) postPath else "/" + postPath
    val pathList = pathGenerator.generate(pathInfo, timeInterval, !failOnMissingPartition, postfixPath)
    val timeFormatString = pathInfo.datePathPattern

    val dataframes = pathList.map(path => {
      val timeStr = path.substring(path.length - (timeFormatString.length + postfixPath.length), path.length - postfixPath.length)
      val time = OfflineDateTimeUtils.createTimeFromString(timeStr, timeFormatString)
      val interval = DateTimeInterval.createFromInclusive(time, time, dateTimeResolution)
      val df = fileLoaderFactory.create(path).loadDataFrame()
      (df, interval)
    })

    if (dataframes.isEmpty) {
      val errMsg = s"Input data is empty for creating TimeSeriesSource. No available " +
        s"date partition exist in HDFS for path ${pathInfo.basePath} between ${timeInterval.getStart} and ${timeInterval.getEnd} "
      val errMsgPf = errMsg + s"with postfix path ${postfixPath}"
      if (postfixPath.isEmpty) {
        throw new FeathrInputDataException(
          ErrorLabel.FEATHR_USER_ERROR, errMsg)
      } else {
        throw new FeathrInputDataException(
          ErrorLabel.FEATHR_USER_ERROR, errMsgPf)
      }
    }
    val datePartitions = dataframes.map {
      case (df, interval) =>
        DatePartition(df, interval)
    }
    new PathPartitionedTimeSeriesSourceAccessor(
      ss,
      datePartitions,
      source,
      timeInterval,
      dateTimeResolution,
      failOnMissingPartition,
      addTimestampColumn,
      partitionLimiter)
  }

}

/**
 * source that contains only a single day/hour data
 *
 * @param df           dataframe format of the data
 * @param dateInterval datetime of this single data/hour data
 */
private[offline] case class DatePartition(df: DataFrame, dateInterval: DateTimeInterval)
