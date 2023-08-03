package com.linkedin.feathr.offline.util.datetime

import com.linkedin.feathr.common.DateTimeResolution.DateTimeResolution
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.common.{DateParam, DateTimeResolution}
import com.linkedin.feathr.offline.config.ObservationDataTimeSetting

import java.text.SimpleDateFormat
import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.TimeZone

/**
 * A set of static common utility methods and constants related to date time.
 */
private[offline] object OfflineDateTimeUtils {

  /**
   * Method overloaded with the [[ObservationDataTimeSetting]] object
   * @param observationDataTimeSettings [[ObservationDataTimeSetting]] object
   * @return  [[DateTimeInterval]] object
   */
  def createTimeIntervalFromDateTimeRange(observationDataTimeSettings: ObservationDataTimeSetting): DateTimeInterval = {
    createTimeIntervalFromDateParam(Some(observationDataTimeSettings.dateParam), observationDataTimeSettings.timeFormat)
  }

  val DEFAULT_TIMEZONE: String = "America/Los_Angeles" // default timezone PDT/PST
  val DEFAULT_ZONE_ID: ZoneId = ZoneId.of(DEFAULT_TIMEZONE)
  val DEFAULT_TIME_FORMAT = "yyyyMMdd"
  val DEFAULT_HOURLY_TIME_FORMAT = "yyyyMMddHH"

  /**
   * date partition filter that can be used in the Spark reader APIs
   *
   * @param interval the time input date timeRange
   * @param dateTimeResolution the date time resolution of the underlay dataset: whether it's daily or hourly.
   * @return date partition filter that can be used in the Spark reader APIs
   */
  def dateRange(interval: DateTimeInterval, dateTimeResolution: DateTimeResolution): String = {
    val adjusted = interval.adjustWithDateTimeResolution(dateTimeResolution)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH").withZone(DEFAULT_ZONE_ID)
    val localStartTime = formatter.format(adjusted.getStart)
    val localEndTime = formatter.format(adjusted.getEnd)
    s"datepartition >= '$localStartTime' and datepartition < '$localEndTime'"
  }

  /**
   * Calculate the observation data time range with fact data time range:
   *
   * fact data start time = observation data start time - window - maxDelay
   * fact data end time = observation data end time - minDelay
   *
   * @param obsTimeRange time range of observation data
   * @param window length of window
   * @param timeDelays an array with delay durations
   * @return new time range with the window and time delay applied
   *
   */
  def getFactDataTimeRange(obsTimeRange: DateTimeInterval, window: Duration, timeDelays: Array[Duration]): DateTimeInterval = {
    val minTimeDelay = if (timeDelays.isEmpty) Duration.ZERO else timeDelays.min
    val maxTimeDelay = if (timeDelays.isEmpty) Duration.ZERO else timeDelays.max

    // notice that the durations are in seconds. So the time may shift for duration of day if it crosses the daylight saving.
    val newStartTime = obsTimeRange.getStart.minus(window).minus(maxTimeDelay)
    val newEndTime = obsTimeRange.getEnd.minus(minTimeDelay)
    new DateTimeInterval(newStartTime, newEndTime)
  }

  /**
   * translate a certain string to a [[ZonedDateTime]], according to the format and timezone
   * @param dateString input date string
   * @param formatString the format of the input date string
   * @param tz the time zone
   * @return a ZonedDateTime
   */
  def createTimeFromString(dateString: String, formatString: String = DEFAULT_TIME_FORMAT, tz: String = DEFAULT_TIMEZONE): ZonedDateTime = {
    // we still need to use the old Java API to parse the date time string.
    // the new DataTimeFormat API fails when parsing a date only string to ZonedDateTime.
    val format = new SimpleDateFormat(formatString)
    format.setTimeZone(TimeZone.getTimeZone(tz))
    // we support source path as both /path/to/datasets/daily and /path/to/datasets/daily/, and /path//to/data,
    // so need to strip potential '/' and replace '//' with '/'
    val date = format.parse(dateString.stripMargin('/').replaceAll("//", "/"))
    ZonedDateTime.ofInstant(date.toInstant, ZoneId.of(tz))
  }

  /**
   * create time interval from a [[DateParam]] object.
   *
   * this will only create a valid time interval if either one of the two is true:
   * 1. both startDate and endDate are set
   * 2. numDays is set and dateOffset (if present) is a non-negative integer and numDays is positive integer
   * any other parameter setting will trigger an exception
   * in the case of 2, the startDate will be today_in_pst - dateOffset - numDays + 1, endDate will be today_in_pst - dateOffset
   *
   * Notice the end date is inclusive in the input DataParam but exclusive in the output interval.
   *

   * @param dateParamOpt input [[DateParam]] object which contains the absolute time range or relative time range
   * @param timeFormatOpt Format of the timestamp when absoluteTimeRange is provided. Default is yyyyMMdd
   * @param targetDate specifies the reference date for the "offset" and "numDates" to be computed based on
   * @return a DateTimeInterval
   */
  def createTimeIntervalFromDateParam(dateParamOpt: Option[DateParam], timeFormatOpt: Option[String], targetDate: Option[String] = None): DateTimeInterval = {
    if (dateParamOpt.isEmpty) {
      throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"Trying to create time Interval from DateParam. Date is not defined. Please provide date.")
    }
    val dateParam = dateParamOpt.get

    val timeFormat = timeFormatOpt.getOrElse(DEFAULT_TIME_FORMAT)
    val absoluteDateSet = dateParam.startDate.nonEmpty && dateParam.endDate.nonEmpty
    val relativeDateSet = dateParam.numDays.nonEmpty

    if (absoluteDateSet && !relativeDateSet) {
      val startDate = createTimeFromString(dateParam.startDate.get, timeFormat)
      val endDate = createTimeFromString(dateParam.endDate.get, timeFormat)
      DateTimeInterval.createFromInclusive(startDate, endDate, getDateTimeResolutionFromPattern(timeFormat))
    } else if (relativeDateSet && !absoluteDateSet) {
      /* We will parse the numDays parameter to check if the format is in hourly or daily format due to the following reasons:-
       * a. numDays and offset should have the same format (both should be in days or in hours)
       * b. Daily and hourly are the only the two formats we support, so cannot expect the user to pass in the formats
       *    in either minutes or seconds.
       */
      val offset = DateTimePeriod.parse(dateParam.dateOffset getOrElse ("0h")) // Offset should not be mandatory, we default it 0h
      val window = DateTimePeriod.parse(dateParam.numDays.get)

      if (offset.length < 0) {
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"Trying to create a valid time interval. " +
            s"dateOffset($dateParam.dateOffset) should be non-negative. Please provide non-negative dateOffset.")
      }

      if (window.length <= 0) {
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"Trying to create a valid time interval." +
            s"numDays($dateParam.numDays) should be positive." +
            s"Please provide a positive numDays.")
      }

      val resolution = window.unit
      val baseDate = if (targetDate.isDefined) {
        // if the target Date is defined, use this date instead of TODAY
        OfflineDateTimeUtils.createTimeFromString(targetDate.get, timeFormat)
      } else {
        ZonedDateTime.now(OfflineDateTimeUtils.DEFAULT_ZONE_ID).truncatedTo(dateTimeResolutionToChronoUnit(resolution))
      }

      val endDate = baseDate.minus(offset.length, dateTimeResolutionToChronoUnit(offset.unit))
      val startDate = endDate.minus(window.length - 1, dateTimeResolutionToChronoUnit(window.unit))

      DateTimeInterval.createFromInclusive(startDate, endDate, resolution)
    } else {
      throw new FeathrConfigException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"Trying to create a valid time interval." +
          s"The provided format is incorrect: dateParam - $dateParam, targetDate - $targetDate" +
          s"Please either set both startDate and endDate, or both numDays and dateOffset. Other parameter combinations are not accepted.")
    }
  }

  /**
   * The [[DateParam]] should have inclusive end date but it's used as exclusive end date for feature generation.
   * But when it's used for non-SWA features, esp. when the length in  [[DateTimeConfig]] is 0,
   * it works as an inclusive range.
   * So here we need to specially handle it. In the long term, we'd better clarify the feature generation configurations.
   * See: FeatureGenKeyTagAnalyzer.convertToJoiningFeatureParams, DateTimeParam.startDateTimeInclusive, DateTimeParam.endDateTimeInclusive
   * @param dateParam
   * @return
   */
  def createIntervalFromFeatureGenDateParam(dateParam: DateParam): DateTimeInterval = {
    val (timeFormat, resolution) = dateParam.getResolution match {
      case ChronoUnit.DAYS => (DEFAULT_TIME_FORMAT, DateTimeResolution.DAILY)
      case ChronoUnit.HOURS => (DEFAULT_HOURLY_TIME_FORMAT, DateTimeResolution.HOURLY)
    }

    val startDate = createTimeFromString(dateParam.startDate.get, timeFormat)
    val endDate = createTimeFromString(dateParam.endDate.get, timeFormat)
    if (startDate == endDate) {
      // this is for non-SWA feature when the length in the config is 0.
      DateTimeInterval.createFromInclusive(startDate, endDate, resolution)
    } else {
      new DateTimeInterval(startDate, endDate)
    }
  }

  /**
   * truncate the epoch to the start of the day or start of the hour in America/Los_Angeles time zone.
   * This function is for time-aware join and it will be used in a Spark UDF.
   * The original approach using mod would group the dates in UTC timezone, which is not expected.
   *
   * @param epochSecond epoch in second
   * @param isDaily whether truncate to day or hour
   * @return truncated epoch in second
   */
  def truncateEpochSecond(epochSecond: Long, isDaily: Boolean): Long = {
    ZonedDateTime
      .ofInstant(Instant.ofEpochSecond(epochSecond), DEFAULT_ZONE_ID)
      .truncatedTo(if (isDaily) ChronoUnit.DAYS else ChronoUnit.HOURS)
      .toEpochSecond
  }

  /**
   * infer the date time resolution from the data time pattern.
   * We use a simple rule here: if the pattern contain H/h/K/k, which stands for hour, then it's an hourly; otherwise, it's daily.
   * @param pattern the data time format pattern string
   * @return the date time resolution, daily or hourly
   */
  def getDateTimeResolutionFromPattern(pattern: String): DateTimeResolution = {
    if (pattern.matches(".*[HhKk].*")) DateTimeResolution.HOURLY else DateTimeResolution.DAILY
  }

  /**
   * convert DateTimeResolution to ChronoUnit
   * @param dateTimeResolution input dateTimeResolution
   * @return the corresponding ChronoUnit
   */
  def dateTimeResolutionToChronoUnit(dateTimeResolution: DateTimeResolution): ChronoUnit = {
    dateTimeResolution match {
      case DateTimeResolution.DAILY => ChronoUnit.DAYS
      case DateTimeResolution.HOURLY => ChronoUnit.HOURS
    }
  }
}
