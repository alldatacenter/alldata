package com.linkedin.feathr.offline.util.datetime

import com.linkedin.feathr.common.DateTimeResolution
import com.linkedin.feathr.common.DateTimeResolution.DateTimeResolution
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException}


/**
 * Case class to represent time period with the length and unit, such as 1 day, or 2 hours.
 * It's used for time-based join where users can specify the time range for each features in the join config.
 * Feathr will load the source data according to the time range and then join with observation.
 * In our use cases, we ony support day and hour. It can be extended to support other resolution if needed.
 *
 * The different from Java's Duration is that the Duration is measured in seconds.
 * Thus a Duration of one day is always exactly 24 hours. So it will shift the time during daylight saving switch.
 * We didn't use Java's Period because it doesn't support hourly resolution.
 *
 * Similar logic exists in WindowTimeUnit. Due to the differences above, we don't reuse that class.
 *
 * @param length the length of the period
 * @param unit the time unit of the period, can be day or hour.
 */
private[offline] case class DateTimePeriod(length: Long, unit: DateTimeResolution)

private[offline] object DateTimePeriod {

  /**
   * parse the string such as "1d" or "2h" to a DateTimeRange object
   */
  def parse(dateTimeRangeString: String): DateTimePeriod = {
    val trimmedString = dateTimeRangeString.trim
    try {
      val timeUnit = trimmedString.takeRight(1).toUpperCase match {
        case "D" => DateTimeResolution.DAILY
        case "H" => DateTimeResolution.HOURLY
      }
      val length = trimmedString.dropRight(1).trim.toLong
      DateTimePeriod(length, timeUnit)
    } catch {
      case _: Exception =>
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"$dateTimeRangeString is not a valid DateTimePeriod. The correct example can be '1d'(1 day) or '2h'(2 hours)")
    }
  }
}
