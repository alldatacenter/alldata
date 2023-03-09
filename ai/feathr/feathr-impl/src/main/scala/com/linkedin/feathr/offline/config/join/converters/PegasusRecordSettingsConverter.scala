package com.linkedin.feathr.offline.config.join.converters

import com.linkedin.data.template.GetMode
import com.linkedin.feathr.common.DateParam
import com.linkedin.feathr.config.join.{InputDataTimeSettings, JoinTimeSettings, Settings}
import com.linkedin.feathr.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.offline.anchored.WindowTimeUnit
import com.linkedin.feathr.offline.config.{JoinConfigSettings, JoinTimeSetting, ObservationDataTimeSetting, TimestampColumn}
import com.linkedin.feathr.offline.util.datetime.OfflineDateTimeUtils

/**
 * trait for converting PDL [[Settings]] of the [[FrameJoinConfig]] to offline's [[JoinConfigSettings]]
 */
private[converters] trait PegasusRecordSettingsConverter {

  /**
   * Convert PDL [[Settings]] of the [[FrameJoinConfig]] to offline's [[JoinConfigSettings]]
   */
  def convert(settings: Settings): JoinConfigSettings
}

/**
 * default implementation of PegasusRecordSettingsConverter.
 */
private[converters] object PegasusRecordSettingsConverter extends PegasusRecordSettingsConverter {

  /**
   * Convert PDL [[Settings]] of the [[FrameJoinConfig]] to offline's [[JoinConfigSettings]]
   */
  override def convert(settings: Settings): JoinConfigSettings = {
    val inputDataTimeSettings = Option(settings.getInputDataTimeSettings(GetMode.DEFAULT)).map(convertInputDataTimeSettings)
    val joinTimeSetting = Option(settings.getJoinTimeSettings(GetMode.DEFAULT)).map(convertJoinTimeSettings)
    JoinConfigSettings(inputDataTimeSettings, joinTimeSetting, None)
  }

  /**
   * Convert PDL[[JoinTimeSettings]] to offline's [[JoinTimeSetting]]
   */
  private def convertJoinTimeSettings(joinTimeSettings: JoinTimeSettings): JoinTimeSetting = {
    if (joinTimeSettings.isTimestampColJoinTimeSettings) {
      val settings = joinTimeSettings.getTimestampColJoinTimeSettings
      val pdlTimestampColumn = settings.getTimestampColumn
      val timestampColumnDefinition = if (pdlTimestampColumn.getDefinition.isColumnName) {
        pdlTimestampColumn.getDefinition.getColumnName
      } else {
        pdlTimestampColumn.getDefinition.getSparkSqlExpression.getExpression
      }
      val timeStampColumn = TimestampColumn(timestampColumnDefinition, pdlTimestampColumn.getFormat)
      val simulateTimeDelay =
        Option(settings.getSimulateTimeDelay(GetMode.DEFAULT)).map(delay =>
          WindowTimeUnit.parseWindowTime(PegasusRecordDateTimeConverter.convertDuration(delay.getLength, delay.getUnit)))
      JoinTimeSetting(timeStampColumn, simulateTimeDelay, useLatestFeatureData = false)
    } else if (joinTimeSettings.isUseLatestJoinTimeSettings) {
      val useLatestFeatureData = joinTimeSettings.getUseLatestJoinTimeSettings.isUseLatestFeatureData
      JoinTimeSetting(TimestampColumn("", ""), None, useLatestFeatureData)
    } else {
      throw new FeathrConfigException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"joinTimeSettings $joinTimeSettings should have either SettingsWithTimestampCol or SettingsWithUseLatestFeatureData.")
    }
  }

  /**
   * Convert PDL[[ObservationDataTimeSettings]] to offline's [[ObservationDataTimeSetting]]
   */
  private def convertInputDataTimeSettings(inputDataTimeSettings: InputDataTimeSettings): ObservationDataTimeSetting = {
    val timeRange = inputDataTimeSettings.getTimeRange
    if (timeRange.isAbsoluteTimeRange) {
      val absoluteTimeRange = timeRange.getAbsoluteTimeRange
      val startTime = absoluteTimeRange.getStartTime
      val endTime = absoluteTimeRange.getEndTime
      if (!((startTime.isDate && endTime.isDate) || (startTime.isHourTime && endTime.isHourTime))) {
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"AbsoluteTimeRange $absoluteTimeRange has different granularity for startTime and endTime. One is daily and the other is hourly.")
      }
      val formatString = if (startTime.isDate) OfflineDateTimeUtils.DEFAULT_TIME_FORMAT else OfflineDateTimeUtils.DEFAULT_HOURLY_TIME_FORMAT
      val startTimeString = if (startTime.isDate) {
        PegasusRecordDateTimeConverter.convertDate(startTime.getDate, formatString)
      } else {
        PegasusRecordDateTimeConverter.convertHourTime(startTime.getHourTime, formatString)
      }
      val endTimeString = if (endTime.isDate) {
        PegasusRecordDateTimeConverter.convertDate(endTime.getDate, formatString)
      } else {
        PegasusRecordDateTimeConverter.convertHourTime(endTime.getHourTime, formatString)
      }
      val dateParam = DateParam(Some(startTimeString), Some(endTimeString))
      ObservationDataTimeSetting(dateParam, Some(formatString))
    } else if (timeRange.isRelativeTimeRange) {
      val relativeTimeRange = timeRange.getRelativeTimeRange
      val offset = PegasusRecordDateTimeConverter.convertDuration(relativeTimeRange.getOffset, relativeTimeRange.getWindow.getUnit)
      val window = PegasusRecordDateTimeConverter.convertDuration(relativeTimeRange.getWindow.getLength, relativeTimeRange.getWindow.getUnit)
      val dateParam = DateParam(None, None, Some(offset), Some(window))
      ObservationDataTimeSetting(dateParam, None)
    } else {
      throw new FeathrConfigException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"RelativeTimeRange and AbsoluteTimeRange are not set in InputDataTimeSettings $inputDataTimeSettings. " +
          "If intention is to not restrict the size of the input data, please remove the inputDataTimeSettings section completely.")
    }
  }
}
