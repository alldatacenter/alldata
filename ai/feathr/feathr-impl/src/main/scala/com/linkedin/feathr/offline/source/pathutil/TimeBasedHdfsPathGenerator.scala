package com.linkedin.feathr.offline.source.pathutil

import com.linkedin.feathr.offline.util.datetime.{DateTimeInterval, OfflineDateTimeUtils}

import java.time.format.DateTimeFormatter

/**
 * Generate a list of paths based on the given time interval
 * @param pathChecker the path checker is used to check whether a file path exists.
 */
private[offline] class TimeBasedHdfsPathGenerator(pathChecker: PathChecker) {

  /**
   * Helper function for generating file names for daily and hourly format data
   * Supported path format include:
   * 1. Daily data can be AVRO data following in HomeDir/daily/yyyy/MM/dd folder
   * 2. Orc/Hive data following in HomeDir/datepartition=yyyy-MM-dd-00
   * Example:
   * base path: foo/bar, return foo/bar/daily/2020/06/02/00 or foo/bar/datepartition=2020-06-02-00
   *
   * @param pathInfo the pathInfo analyzed by TimeBasedHdfsPathAnalyzer
   * @param timeInterval the time interval to generate the path list.
   * @param ignoreMissingFiles if set to true, the missing files will be removed from the returned list.
   * @return a sequence of paths with date
   */
  def generate(pathInfo: PathInfo, timeInterval: DateTimeInterval, ignoreMissingFiles: Boolean, postfixPath: String = ""): Seq[String] = {
    val dateTimeResolution = pathInfo.dateTimeResolution
    val adjustedInterval = timeInterval.adjustWithDateTimeResolution(dateTimeResolution)
    val factDataStartTime = adjustedInterval.getStart
    val factDataEndTime = adjustedInterval.getEnd
    val chronUnit = OfflineDateTimeUtils.dateTimeResolutionToChronoUnit(dateTimeResolution)
    val numUnits = chronUnit.between(factDataStartTime, factDataEndTime).toInt
    val formatter = DateTimeFormatter.ofPattern(pathInfo.datePathPattern).withZone(OfflineDateTimeUtils.DEFAULT_ZONE_ID)
    val filePaths = (0 until numUnits)
        .map(offset => pathInfo.basePath + formatter.format(factDataStartTime.plus(offset, chronUnit)) + postfixPath).distinct

    if (ignoreMissingFiles) {
      filePaths.filter(pathChecker.exists)
    } else {
      filePaths
    }
  }
}
