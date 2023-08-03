package com.linkedin.feathr.offline.util

import com.linkedin.feathr.common.DateTimeUtils
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrInputDataException}
import com.linkedin.feathr.offline.util.HdfsUtils.createStringPath
import com.linkedin.feathr.offline.util.datetime.{DateTimeInterval, OfflineDateTimeUtils}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZonedDateTime}
import java.util.TimeZone

// built-in output processor name in feature generation
private[offline] object FeatureGenConstants {
  val HDFS_OUTPUT_PROCESSOR_NAME = "HDFS"
  val REDIS_OUTPUT_PROCESSOR_NAME = "REDIS"
  val MONITORING_OUTPUT_PROCESSOR_NAME = "MONITORING"
  val OUTPUT_TIME_PATH = "outputTimePath"
  val SAVE_SCHEMA_META = "saveSchemaMeta"
  val WORK_DIR = "workDir"
  val OUTPUT_WITH_TIMESTAMP = "outputTimestamp" // parameter to check if the output should include timestamp
  val OUTPUT_TIMESTAMP_FORMAT = "outputTimestampFormat" // output timestamp format
  val DEFAULT_OUTPUT_TIMESTAMP_FORMAT = "yyyyMMdd"
  val END_TIME_FORMAT = "yyyy/MM/dd"
  val FEATHR_AUTO_GEN_TIMESTAMP_FIELD = "feathrAutoGenTimestamp"
}

/**
 * Utility functions for applying incremental aggregation in FeatureJoin. The current incremental aggregation
 * only supports daily resolution. The key idea of incremental aggregation is to utilize the previous aggregated
 * snapshot. We update it through adding the new delta window and subtracting the old delta window.
 */
private[offline] object IncrementalAggUtils {
  private final val conf: Configuration = new Configuration()
  private final val LATEST_PATTERN = "#LATEST"
  private final val fs: FileSystem = FileSystem.get(conf)

  /**
   * Get the path of latest aggregation snapshot. The current incremental aggregation only supports daily
   * resolution and all previous aggregation snapshots are organized as /rootDir/yyyy/MM/dd
   * @param rootAggDir root directory of the previous aggregated snapshots
   * @param cutOffDate returned date string should has date before this cut off date
   * @return the path of latest aggregation snapshot
   */
  def getLatestHistoricalAggregationPath(rootAggDir: String, cutOffDate: LocalDateTime): Option[String] = {
    AclCheckUtils.getLatestPath(fs, rootAggDir + s"${LATEST_PATTERN}${LATEST_PATTERN}${LATEST_PATTERN}", cutOffDate)
  }

  /**
   * Get the latest date with aggregation snapshot. The current incremental aggregation only supports daily
   * resolution and all aggregation snapshots are organized as /rootDir/yyyy/MM/dd
   * @param rootAggDir root directory of the previous aggregated snapshots
   * @param cutOffDate returned date should has date before this cut off date
   * @return the latest date having aggregation snapshot
   */
  def getLatestHistoricalAggregationDate(rootAggDir: String, cutOffDate: LocalDateTime): Option[ZonedDateTime] = {
    val resolvedLatestPathName = getLatestHistoricalAggregationPath(rootAggDir, cutOffDate)
    resolvedLatestPathName.map(pathName => {
      val latestDateStr = pathName.drop(rootAggDir.length + 1)
      OfflineDateTimeUtils.createTimeFromString(latestDateStr, "yyyy/MM/dd")
    })
  }

  /**
   * Get the interval of the old delta window.
   * @param rootAggDir root directory of the previous aggregation snapshots.
   * @param aggWindow the size of the aggregation window
   * @param incrementalWindow the size of the incremental window
   * @param newDeltaEndDateStr new delta start date in str of yyyyMMdd
   * @return the interval of the old delta window  (left inclusive, right exclusive)
   */
  def getOldDeltaWindowDateParam(rootAggDir: String, aggWindow: Int, incrementalWindow: Int, newDeltaEndDateStr: String): DateTimeInterval = {
    val endDate = OfflineDateTimeUtils.createTimeFromString(newDeltaEndDateStr, "yyyyMMdd")
    val start = endDate.minusDays(aggWindow + incrementalWindow)
    val end = endDate.minusDays(aggWindow)
    new DateTimeInterval(start, end)
  }

  /**
   * Get the interval of the new delta window and the size of the interval.
   * @param rootAggDir root directory of the previous aggregation snapshots.
   * @param aggWindow the size of the aggregation window
   * @param newDeltaWindowEndDateStr the end date of the new delta window
   * @return the interval of the new delta window and its size (left inclusvie, right exclusive)
   */
  def getNewDeltaWindowInterval(rootAggDir: String, aggWindow: Int, newDeltaWindowEndDateStr: String): (DateTimeInterval, Long) = {
    val endDate = OfflineDateTimeUtils.createTimeFromString(newDeltaWindowEndDateStr, "yyyyMMdd").toLocalDateTime
    val startDateBasedOnPreAgg = IncrementalAggUtils.getNewDeltaWindowStartDate(rootAggDir, endDate)
    val startDateBasedOnPreAggStr = IncrementalAggUtils.formatDateAsString(startDateBasedOnPreAgg)
    val startDateBasedOnRuntime =
      DateTimeUtils.minusTime(newDeltaWindowEndDateStr, "yyyyMMdd", "yyyyMMdd", TimeZone.getTimeZone("America/Los_Angeles"), aggWindow, ChronoUnit.DAYS)
    val newDeltaWindowStartDateStr = if (startDateBasedOnRuntime > startDateBasedOnPreAggStr) startDateBasedOnRuntime else startDateBasedOnPreAggStr
    val newDeltaWindowStartDate = OfflineDateTimeUtils.createTimeFromString(newDeltaWindowStartDateStr, "yyyyMMdd")
    val newDeltaWindowEndDate = OfflineDateTimeUtils.createTimeFromString(newDeltaWindowEndDateStr, "yyyyMMdd")
    val incrementalWindowSize = ChronoUnit.DAYS.between(newDeltaWindowStartDate, newDeltaWindowEndDate)
    val interval = new DateTimeInterval(newDeltaWindowStartDate, newDeltaWindowEndDate)
    (interval, incrementalWindowSize)
  }

  /**
   * Format a date to string and the default string pattern is yyyy--MM--dd.
   * @param date the date to be formatted
   * @param pattern the format pattern
   * @return the formatted date
   */
  def formatDateAsString(date: ZonedDateTime, pattern: String = "yyyyMMdd"): String = {
    val formatter = DateTimeFormatter.ofPattern(pattern).withZone(OfflineDateTimeUtils.DEFAULT_ZONE_ID)
    formatter.format(date)
  }

  /**
   * transform a date string from source format to destination format.
   * @param sourceDateStr the date string
   * @param sourcePattern the source format
   * @param destPattern the destination format
   * @return a string with destination format
   */
  def transformDateString(sourceDateStr: String, sourcePattern: String = "yyyyMMdd", destPattern: String = "yyyy/MM/dd"): String = {
    val sourceDate = OfflineDateTimeUtils.createTimeFromString(sourceDateStr, sourcePattern)
    formatDateAsString(sourceDate, destPattern)
  }

  /**
   * Gets all the subfolders paths in a given `basePath`

   * @param basePath The basePath where to search for subfolders
   * @return A List[String] containing the paths of the subfolders sorted in reverse lexicographical order
   */
  def getSubfolderPaths(basePath: String, excludeDirsPrefixList: Seq[String] = List(".", "_"), conf: Configuration = conf): Seq[String] = {
    val fs: FileSystem = FileSystem.get(conf)
    val directories: Seq[String] = fs
      .listStatus(new Path(basePath))
      .filter(_.isDirectory)
      .map(_.getPath.getName)
      .filter(dirName => !excludeDirsPrefixList.exists(prefix => dirName.startsWith(prefix)))
      .map(createStringPath(basePath, _))

    directories
  }

  /**
   * Gets all dataframe paths in a given aggregation snapshot `basePath`
   * @param basePath The basePath where to search for dataframe paths
   * @param cutOffDate load the latest agg snapshot before cutoffdate
   * @return containing the dataframe path
   */
  def getLatestAggSnapshotDFPath(basePath: String, cutOffDate: LocalDateTime): Option[String] = {
    IncrementalAggUtils.getLatestHistoricalAggregationPath(basePath, cutOffDate)
  }

  /**
   *
   * @param basePath The basePath where to search for dataframe paths
   * @param endTime a string indicates the run time specified by the user
   * @param endTimeFormat the string format of the endTime
   * @return the day gaps between the date of the latest aggregation snapshot and the end time
   */
  def getDaysGapBetweenLatestAggSnapshotAndEndTime(basePath: String, endTime: String, endTimeFormat: String): Option[Long] = {
    val endDate = OfflineDateTimeUtils.createTimeFromString(endTime, endTimeFormat).toLocalDateTime
    val latestAggDate = getLatestHistoricalAggregationDate(basePath, endDate)
    latestAggDate.map(aggDate => ChronoUnit.DAYS.between(aggDate.toInstant, OfflineDateTimeUtils.createTimeFromString(endTime, endTimeFormat).toInstant))
  }

  /**
   * Get the start date of the new delta window.
   * @param rootAggDir root directory of the previous aggregation snapshots.
   * @param cutOffDate returned date should has date before this cut off date
   * @return the start date of the new delta window.
   */
  private def getNewDeltaWindowStartDate(rootAggDir: String, cutOffDate: LocalDateTime): ZonedDateTime = {
    val latestDateWithAgg = getLatestHistoricalAggregationDate(rootAggDir, cutOffDate)
    latestDateWithAgg
      .getOrElse(
        throw new FeathrInputDataException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"Cannot getNewDeltaWindowStartDate for path ${rootAggDir} with cutOffDate ${cutOffDate}"))
  }
}
