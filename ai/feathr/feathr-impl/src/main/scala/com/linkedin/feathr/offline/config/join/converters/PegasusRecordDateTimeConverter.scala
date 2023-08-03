package com.linkedin.feathr.offline.config.join.converters

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import com.linkedin.feathr.config.join.{Date, HourTime, TimeUnit}
import com.linkedin.feathr.exception.{ErrorLabel, FeathrConfigException}

private[converters] object PegasusRecordDateTimeConverter {

  /**
   * convert PDL duration with a length and time unit to DateParam's string representation, e.g., 1d or 2h
   */
  def convertDuration(length: Long, unit: TimeUnit): String = {
    unit match {
      case TimeUnit.DAY => s"${length}d"
      case TimeUnit.HOUR => s"${length}h"
      case TimeUnit.MINUTE => s"${length}m"
      case TimeUnit.SECOND => s"${length}s"
      case _ =>
        throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"Invalid TimeUnit $unit. It should be DAY, HOUR, MINUTE or SECOND.")
    }
  }

  /**
   * convert PDL [[Date]] object to string with the given format
   * @param date the PDL date object
   * @param format the date pattern described in [[DateTimeFormatter]], e.g., yyyyMMdd
   * @return the date string, e,g. "20201113"
   */
  def convertDate(date: Date, format: String): String = {
    LocalDate.of(date.getYear, date.getMonth, date.getDay).format(DateTimeFormatter.ofPattern(format))
  }

  /**
   * convert PDL [[HourTime]] object to string with the given format
   * @param hourTime the PDL hourly time object
   * @param format the date pattern described in [[DateTimeFormatter]], e.g, yyyyMMddHH
   * @return the time string, e.g, 2020111310
   */
  def convertHourTime(hourTime: HourTime, format: String): String = {
    LocalDateTime.of(hourTime.getYear, hourTime.getMonth, hourTime.getDay, hourTime.getHour, 0).format(DateTimeFormatter.ofPattern(format))
  }
}
