package com.linkedin.feathr.common

import java.text.SimpleDateFormat
import java.time.temporal.ChronoUnit
import java.util.{Calendar, TimeZone}

private[feathr] object DateTimeUtils {

  // convert DateTimeParam to DateParam
  def toDateParam(dateTimeParam: DateTimeParam): DateParam = {
    DateParam(Some(dateTimeParam.startDateTimeInclusive()), Some(dateTimeParam.endDateTimeInclusive()))
  }

  /**
    * minus time string by a few days/hour depending on the output time format. For example, If the output format has hourly granularity,
    * then the time to subtract will be assumed to be in hours.
    * @param time input time string
    * @param inputTimeFormat input time string format
    * @param outputTimeFormat output time format
    * @param timeZone time zone of input and output time
    * @param timeToSubtract days/hour to minus from input time. This is inferred if it is days or hours from the output time format
    * @return output time string
    */
  def minusTime(time: String, inputTimeFormat: String, outputTimeFormat: String, timeZone: TimeZone, timeToSubtract: Int, resolution: ChronoUnit): String = {
    val inputFormat = new SimpleDateFormat(inputTimeFormat)
    inputFormat.setTimeZone(timeZone)
    val outputFormat = new SimpleDateFormat(outputTimeFormat)
    outputFormat.setTimeZone(timeZone)
    val date = inputFormat.parse(time)
    val startDate = Calendar.getInstance()
    startDate.setTimeZone(timeZone)
    startDate.setTime(date)
    resolution match {
      case ChronoUnit.DAYS => startDate.add(Calendar.DATE, -timeToSubtract)
      case ChronoUnit.HOURS => startDate.add(Calendar.HOUR, -timeToSubtract)
    }
    outputFormat.format(startDate.getTime)
  }
}
