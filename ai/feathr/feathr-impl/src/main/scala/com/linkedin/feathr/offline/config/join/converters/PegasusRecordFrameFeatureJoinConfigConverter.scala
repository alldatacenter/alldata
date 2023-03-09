package com.linkedin.feathr.offline.config.join.converters

import com.linkedin.data.template.GetMode
import com.linkedin.feathr.config.join.{FrameFeatureJoinConfig, JoiningFeature, TimeUnit}
import com.linkedin.feathr.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.offline.config.{FeatureJoinConfig, KeyedFeatureList}
import com.linkedin.feathr.offline.util.datetime.OfflineDateTimeUtils

import scala.collection.JavaConverters._

/**
 * Convert PDL [[FrameFeatureJoinConfig]] to offline's [[FeatureJoinConfig]]
 * @param pegasusRecordSettingsConverter the convert for the settings section of the join config
 */
private[offline] class PegasusRecordFrameFeatureJoinConfigConverter(private val pegasusRecordSettingsConverter: PegasusRecordSettingsConverter) {
  val FEATURE_GROUP_NAME = "FeatureJoinConfigConverterGeneratedGroupName"

  /**
   * Convert PDL [[FrameFeatureJoinConfig]] to offline's [[FeatureJoinConfig]]
   */
  def convert(frameFeatureJoinConfig: FrameFeatureJoinConfig): FeatureJoinConfig = {
    // convert the features
    val joiningFeatures = frameFeatureJoinConfig.getFeatures.asScala
    val features = joiningFeatures.map(convertFeature)
    val groups = Map(FEATURE_GROUP_NAME -> features)
    val settings = Option(frameFeatureJoinConfig.getSettings(GetMode.DEFAULT)).map(pegasusRecordSettingsConverter.convert)
    FeatureJoinConfig(groups, settings)
  }

  /**
   * convert PDL [[JoiningFeature]] to offline's [[KeyedFeatureList]]
   */
  private def convertFeature(feature: JoiningFeature): KeyedFeatureList = {
    val keys = feature.getKeys.asScala

    var startDate: Option[String] = None
    var endDate: Option[String] = None
    var numDays: Option[String] = None
    var dateOffset: Option[String] = None
    if (feature.hasDateRange) {
      val dateRange = feature.getDateRange
      if (dateRange.isAbsoluteDateRange) {
        val absoluteRange = dateRange.getAbsoluteDateRange
        startDate = Some(PegasusRecordDateTimeConverter.convertDate(absoluteRange.getStartDate, OfflineDateTimeUtils.DEFAULT_TIME_FORMAT))
        endDate = Some(PegasusRecordDateTimeConverter.convertDate(absoluteRange.getEndDate, OfflineDateTimeUtils.DEFAULT_TIME_FORMAT))
      } else if (dateRange.isRelativeDateRange) {
        val relativeRange = dateRange.getRelativeDateRange
        numDays = Some(PegasusRecordDateTimeConverter.convertDuration(relativeRange.getNumDays, TimeUnit.DAY))
        dateOffset = Some(PegasusRecordDateTimeConverter.convertDuration(relativeRange.getDateOffset, TimeUnit.DAY))
      } else {
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"RelativeTimeRange and AbsoluteTimeRange are not set in DateRange $dateRange of feature $feature.")
      }
    }

    val featureAliasName = Option(feature.getFeatureAlias())

    val overrideTimeDelay =
      Option(feature.getOverrideTimeDelay(GetMode.DEFAULT)).map(delay => PegasusRecordDateTimeConverter.convertDuration(delay.getLength, delay.getUnit))
    KeyedFeatureList(keys, Seq(feature.getFrameFeatureName), startDate, endDate, dateOffset, numDays, overrideTimeDelay, featureAliasName)
  }
}

/**
 * Default FrameFeatureJoinConfig converter with default settings converter.
 */
object PegasusRecordFrameFeatureJoinConfigConverter extends PegasusRecordFrameFeatureJoinConfigConverter(PegasusRecordSettingsConverter)
