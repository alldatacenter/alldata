package com.linkedin.feathr.common

import com.linkedin.feathr.common.configObj.DateTimeConfig
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrInputDataException}

import java.text.{ParseException, SimpleDateFormat}
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.{Calendar, TimeZone}


/**
  * Date can be specified in two ways: 1. startTime plus endTime, or 2. dateOffset plus numDays
  * in the case of 2, the startTime will be "today_in_pst - dateOffset - numDays + 1",
  * endTime will be "today_in_pst - dateOffset"
  * This case class is used both by feature and observation data.
 *
  * @param startDate in yyyyMMdd or yyyyMMddHH
  * @param endDate in yyyyMMdd or yyyyMMddHH
  * @param dateOffset non-negative integer (either in days or hours, resolution will determine if it is in days or hours).
  *                   Used to calculate the endTime.
  * @param numDays positive integer (either in days or hours, resolution will determine if it is in days or hours).
  *               Used together with dateOffset to calculate the startTime
  */
case class DateParam(startDate: Option[String] = None,
  endDate: Option[String] = None,
  dateOffset: Option[String] = None,
  numDays: Option[String] = None) {
  /**
    * Get the resolution of the DateParam class. If the startDate is defined, the allow formats are yyyyMMdd or yyyyMMddHH.
    * In this case, we will just parse the date string for it's length and conclude if the hour field is also included.
    * If the window field is defined, we parse the window string to determine if the resolution is daily or hourly.
    * If neither, we will just return the default format of daily.
    * @return ChronoUnit of DAYS or HOURS
    */
  def getResolution: ChronoUnit = {
    // We dont need to get into malformed input checking here as the input for this class is only through feathr.
    if(startDate.isDefined) {
      try {
        val sdf = new SimpleDateFormat("yyyyMMdd")
        sdf.setLenient(false)
        sdf.parse(startDate.get)
        ChronoUnit.DAYS
      } catch {
        case e: ParseException =>
          try {
            val sdf = new SimpleDateFormat("yyyyMMddHH")
            sdf.setLenient(false)
            sdf.parse(startDate.get)
            ChronoUnit.HOURS
          } catch {
            case e: ParseException => throw new FeathrInputDataException(ErrorLabel.FEATHR_ERROR, s"The only formats recognised by the DateParam class are 'yyyyMMdd' and 'yyyyMMddHH'." +
              s" ${startDate.get} does not conform to the above formats")
          }
      }
    } else if (numDays.isDefined) {
      if (numDays.get.toString.contains("h") || numDays.get.toString.contains("H")) {
        ChronoUnit.HOURS
      } else if (numDays.get.toString.contains("d") || numDays.get.toString.contains("D")){
        ChronoUnit.DAYS
      } else {
        throw new FeathrInputDataException(ErrorLabel.FEATHR_ERROR,
          s"We only support daily or hourly granularity and should adhere to java.time.Duration time format. ${numDays.get} is invalid.")
      }
    } else {
      // We do not want throw an exception here only because an empty dateParam class is valid. In that case, we can return the default resolution of daily.
      ChronoUnit.DAYS
    }
  }
}

private[feathr] object DateTimeResolution extends Enumeration {
  type DateTimeResolution = Value
  val DAILY, HOURLY = Value
}

/**
  * represent a time window, can be used to define a time aware feature
  * user should use the companion object to create instance of this class
  *
  * @param referenceEndDateTime reference date time for the end of the window
  * @param length               number of units(day/hour) of the window
  * @param isReferenceEndDateTimeInclusive is the referenceEndDateTime inclusive or exclusive
  * @param offset               offset/delay of the reference data time for the end of the window
  * @param timeZone             timezone of the date time, default is pst
  * @param dateTimeFormat       input java time format string
  * @param resolution           is the unit of window in daily or hourly
  *
  * e.g. if referenceEndDateTime is 2018-03-15-00, length is 3, offset is 0,
  * when isReferenceEndDateTimeInclusive is true, start and end date time (both inclusive) are 2019-03-13-00 and 2019-03-15-00
  * when isReferenceEndDateTimeInclusive is false, start and end date time (both inclusive) are 2019-03-12-00 and 2019-03-14-00
  */

private[feathr] class DateTimeParam private(
  // make the default constructor private, so we can validate the parameters in the provided companion object
  val referenceEndDateTime: String,
  val length: Long,
  val isReferenceEndDateTimeInclusive: Boolean,
  val offset: Long,  // offset of endDateTime or referenceDateTime
  val timeZone: TimeZone,
  val dateTimeFormat: String,
  val resolution: ChronoUnit) {
  require(resolution == ChronoUnit.DAYS || resolution == ChronoUnit.HOURS)
  // output time format of startDateTimeInclusive and endDateTimeInclusive
  val outputFormatString = resolution match {
    case ChronoUnit.DAYS => "yyyyMMdd"
    case ChronoUnit.HOURS => "yyyyMMddHH"
    case _ => throw new FeathrInputDataException(ErrorLabel.FEATHR_ERROR, "Only daily or hourly granularity is supported")
  }

  val outputFormat = new SimpleDateFormat(outputFormatString)
  outputFormat.setTimeZone(timeZone)

  /**
    * if isReferenceEndDateTimeInclusive is true, the startTime will be "referenceEndDateTime in timeZone - offset - length + 1",
    * else will be "referenceEndDateTime in timeZone - offset - length"
    * @return start date time of the window (inclusive)
    */
  def startDateTimeInclusive(): String = {
    DateTimeUtils.minusTime(endDateTimeInclusive(), outputFormatString, outputFormatString, timeZone, length.intValue(), resolution)

  }

  /**
    * if isReferenceendTimeTimeInclusive is true, endTime will be "referenceendTimeTime in timeZone - offset",
    * else will be "referenceendTimeTime in timeZone - offset -1"
    *
    * @return end date time of the window (inclusive)
    */
  def endDateTimeInclusive(): String = {
    if (referenceEndDateTime == "NOW") {
      val calendar = Calendar.getInstance(timeZone)
      calendar.setTimeZone(timeZone)
      calendar.add(Calendar.DATE, -offset.intValue())
      outputFormat.format(calendar.getTime)
    } else {
      DateTimeUtils.minusTime(referenceEndDateTime, dateTimeFormat, outputFormatString, timeZone, offset.intValue(), resolution)
    }
  }
}

/**
  * see doc for class [[DateTimeParam]]
  */
private[feathr] object DateTimeParam {

  // create a DateTimeParam object from the config
  def apply(config: DateTimeConfig): DateTimeParam = {
    val offset = config.getOffset.getSeconds / config.getTimeResolution.getDuration.getSeconds
    new DateTimeParam(config.getReferenceEndTime,
      config.getLength,
      true,
      offset,
      config.getTimeZone,
      config.getReferenceEndTimeFormat,
      config.getTimeResolution)
  }

  // create a DateTimeParam object from explicit fields
  def apply(referenceDateTime: String,
    length: Long,
    isEndDateTimeInclusive: Boolean,
    offset: Long = 0,
    timeZone: String = "America/Los_Angeles",
    endDateTimeFormat: String = "yyyy-MM-dd",
    resolution: ChronoUnit = ChronoUnit.DAYS): DateTimeParam = {
      new DateTimeParam(referenceDateTime, length, isEndDateTimeInclusive, offset,
        TimeZone.getTimeZone(timeZone), endDateTimeFormat, resolution)
  }

  /**
    * shift the start time by a duration
    * @param dateTimeParam dateTimeParam to shift
    * @param duration duration to shift
    * @return shifted dateTimeParam
    */
  def shiftStartTime(dateTimeParam: DateTimeParam, duration: Duration): DateTimeParam = {
    new DateTimeParam(dateTimeParam.referenceEndDateTime,
      if (dateTimeParam.resolution == ChronoUnit.DAYS) dateTimeParam.length + duration.toDays else dateTimeParam.length + duration.toHours,
      dateTimeParam.isReferenceEndDateTimeInclusive,
      dateTimeParam.offset,
      dateTimeParam.timeZone,
      dateTimeParam.dateTimeFormat,
      dateTimeParam.resolution)
  }


  /**
  * shift the end time by a duration
  * @param dateTimeParam dateTimeParam to shift
  * @param duration duration to shift
  * @return shifted dateTimeParam
  */
  def shiftEndTime(dateTimeParam: DateTimeParam, duration: Duration): DateTimeParam = {
    new DateTimeParam(dateTimeParam.referenceEndDateTime,
      dateTimeParam.length,
      dateTimeParam.isReferenceEndDateTimeInclusive,
      if (dateTimeParam.resolution == ChronoUnit.DAYS) dateTimeParam.offset + duration.toDays else dateTimeParam.offset + duration.toHours,
      dateTimeParam.timeZone,
      dateTimeParam.dateTimeFormat,
      dateTimeParam.resolution)
  }
}

/**
  * header of the result of the Feathr feature join and feature generation API
  * @param featureInfoMap  map feature name to its related info in the output dataframe
  */
class Header(val featureInfoMap: Map[TaggedFeatureName, FeatureInfo])

/**
  * information about a feature in the feathr feature join and feature generation output dataframe
  * @param columnName the column in the dataframe that represents this feature
  * @param featureType the feature type of this feature
  */
class FeatureInfo(val columnName: String, val featureType: FeatureTypeConfig) {
  override def toString: String = s"columnName: $columnName; featureType: $featureType"
}

/**
  * suffixes used in feature column name by user customized time aware aggregators
  */
object FeathrAggregationConstants {
  val FEATHR_FEATURE_GROUP_COL_NAME_SUFFIX = "group_col"
  val FEATHR_FEATURE_TIMESTAMP_COL_NAME_SUFFIX  = "timestamp_col_swj"
  val FEATHR_FEATURE_GROUP_METRIC_NAME_SUFFIX =  "group_agg_metric"
}

/**
  * All parameters relevant to a feature which is requested to be joined in the join config.
  * @param keyTags  sequence of keyTags of the feature.
  * @param featureName  name of the feature.
  * @param dateParam  time parameters to restrict the size of the joining feature's data. User can set a date range on the feature data.
  * @param timeDelay  the override time delay parameter, which would override the global simulate time delay. This simulates
  *                   a delay on observation data's timestamp by subtracting this value from observation data's timestamp while
  *                   joining this specific feature.
  * @param featureAlias feature alias name, this would be the output of the feature.
  */
case class JoiningFeatureParams (
  keyTags: Seq[String],
  featureName: String,
  dateParam: Option[DateParam] = None,
  timeDelay: Option[String] = None,
  featureAlias: Option[String] = None
)