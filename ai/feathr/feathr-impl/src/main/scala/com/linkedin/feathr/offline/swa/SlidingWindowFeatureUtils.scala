package com.linkedin.feathr.offline.swa

import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.common.{DateTimeResolution, FeatureTypeConfig, FeatureTypes}
import com.linkedin.feathr.{offline, swj}
import com.linkedin.feathr.offline.FeatureDataFrame
import com.linkedin.feathr.offline.anchored.anchorExtractor.TimeWindowConfigurableAnchorExtractor
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.offline.config._
import com.linkedin.feathr.offline.job.PreprocessedDataFrameManager
import com.linkedin.feathr.offline.source.{DataSource, TimeWindowParams}
import com.linkedin.feathr.offline.transformation.FeatureColumnFormat
import com.linkedin.feathr.offline.transformation.FeatureColumnFormat.FeatureColumnFormat
import com.linkedin.feathr.offline.util.FeaturizedDatasetUtils
import com.linkedin.feathr.offline.util.datetime.{DateTimeInterval, OfflineDateTimeUtils}
import com.linkedin.feathr.swj.{FactData, GroupBySpec, LateralViewParams, SlidingWindowFeature, WindowSpec}
import com.linkedin.feathr.swj.aggregate.{AggregationType, AvgAggregate, AvgPoolingAggregate, CountAggregate, CountDistinctAggregate, LatestAggregate, MaxAggregate, MaxPoolingAggregate, MinAggregate, MinPoolingAggregate, SumAggregate}
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions._
import org.apache.spark.util.sketch.BloomFilter

import java.text.SimpleDateFormat
import java.time._

/**
 * A util class for computing time-based sliding window aggregate features (SWA features).
 */
private[offline] object SlidingWindowFeatureUtils {

  private val log = LogManager.getLogger(getClass)

  private val EPOCH = "epoch"
  private val EPOCH_MILLIS = "epoch_millis"
  private val MILLIS_IN_SECOND = 1000

  val TIMESTAMP_WITHOUT_TIMEZONE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"
  val TIMESTAMP_WITH_TIMEZONE_FORMAT = TIMESTAMP_WITHOUT_TIMEZONE_FORMAT + "Z" // Z is timezone pattern letter
  val UTC_TIMEZONE_OFFSET = "-0000" // PDT/PST
  val DEFAULT_TIME_DELAY = "Default-time-delay"
  val TIMESTAMP_PARTITION_COLUMN = "__feathr_timestamp_column_from_partition"

  /**
   * Check if an anchor contains window aggregate features.
   * Note: if an anchor contains window aggregate features, it will not contain other non-aggregate features.
   */
  def isWindowAggAnchor(anchor: FeatureAnchorWithSource): Boolean = {
    anchor.featureAnchor.extractor.isInstanceOf[TimeWindowConfigurableAnchorExtractor]
  }

  /**
   * Get start and end time from observation data for sliding window aggregation
   * @param contextDF observation data
   * @param joinConfigSettingsOpt join config setting
   * @return
   */
  def getObsSwaDataTimeRange(contextDF: DataFrame, joinConfigSettingsOpt: Option[JoinConfigSettings]): (Option[DateTimeInterval], Option[String]) = {
    // extract time window settings
    joinConfigSettingsOpt match {
      case Some(joinConfigSettings) =>
        val timestampCol = if (joinConfigSettings.joinTimeSetting.isDefined) {
          joinConfigSettings.joinTimeSetting.get.timestampColumn.name
        } else {
          ""
        }
        (
          Some(SlidingWindowFeatureUtils.getObsDataTimeRange(contextDF,
            joinConfigSettings.observationDataTimeSetting, joinConfigSettings.joinTimeSetting)),
          Some(timestampCol))
      case _ => (None, None)
    }
  }

  /**
   * extract observation data time range from join config if it's defined, otherwise extract it from observation data
   * @param observationDataTimeSetting The observation data time settings for sliding window aggregate feature.
   * @param joinTimeSetting  the join time settings of the observation data.
   * @return the time range of observation data, as DateTimeInterval.
   */
  def getObsDataTimeRange(
      contextDF: DataFrame,
      observationDataTimeSetting: Option[ObservationDataTimeSetting],
      joinTimeSetting: Option[JoinTimeSetting]): DateTimeInterval = {
    if (observationDataTimeSetting.isDefined) {
      OfflineDateTimeUtils.createTimeIntervalFromDateTimeRange(observationDataTimeSetting.get)
    } else {
      // When latest feature data is requested, there is no timestamp column. Take the latest time from observation.
      if (joinTimeSetting.isDefined && !joinTimeSetting.get.useLatestFeatureData) {
        val timestampExpr =
          SlidingWindowFeatureUtils.constructTimeStampExpr(joinTimeSetting.get.timestampColumn.name, joinTimeSetting.get.timestampColumn.format)
        val minMax = contextDF.agg(min(expr(timestampExpr)), max(expr(timestampExpr))).head()
        val (minEpoch, maxEpoch) = (minMax.getLong(0), minMax.getLong(1))
        // Here's a problem. The dateTimeRange is inclusive[start, end], but it doesn't tell what the resolution is (daily or hourly).
        // So it's hard to tell what is real end time. We use hourly for better granularity.
        // Daily defaults new days to the 0th hour, skipping hours of data sometimes.
        DateTimeInterval.createFromInclusive(Instant.ofEpochSecond(minEpoch), Instant.ofEpochSecond(maxEpoch), DateTimeResolution.HOURLY)
      } else { // take the current timestamp to indicate the latest data
        DateTimeInterval.createFromInclusive(Instant.now(), Instant.now(), DateTimeResolution.HOURLY)
      }
    }
  }

  /**
   * Assemble a SlidingWindowDataDef.FactData from a fact dataset and Feathr anchor config.
   * @param factDF the fact dataset
   * @param anchorConfigs Feathr anchor configs, all of these anchor should share the same data source, keyExtractor and laterviewParams
   * @param selectedFeatureNames selected features to evaluate
   * @return A FactData object, used by sliding-window-join library
   */
  def getFactDataDef(factDF: DataFrame, anchorConfigs: Seq[FeatureAnchorWithSource], delay: Map[String, Duration],
    selectedFeatureNames: Set[String]): FactData = {
    val timeWindowExtractor = anchorConfigs.map(_.featureAnchor.extractor.asInstanceOf[TimeWindowConfigurableAnchorExtractor])
    val feathrFeatureConfigs: Map[String, TimeWindowFeatureDefinition] = timeWindowExtractor.flatMap(_.features).toMap
    val keys = anchorConfigs.head.featureAnchor.sourceKeyExtractor.getKeyColumnNames().map(sk => s"CAST (${sk} as string)")
    val timeWindowParam = getTimeWindowParam(anchorConfigs.head.source)
    val timeStampExpr = constructTimeStampExpr(timeWindowParam.timestampColumn, timeWindowParam.timestampColumnFormat)
    val sparkWindowFeatureConfigs = feathrFeatureConfigs.collect {
      case (featureName: String, featureDef: TimeWindowFeatureDefinition) if (selectedFeatureNames.contains(featureName)) =>
        val timeDelay = delay.get(featureName) match {
          case Some(delay) => delay
          case None =>
            delay.get(SlidingWindowFeatureUtils.DEFAULT_TIME_DELAY) match { // No time delay was specified using the featureOverride.
              case Some(delay) => delay
              case None => Duration.ZERO // No time delay was specified at all.
            }
        }
        convertFeathrDefToSwjDef(featureName, featureDef, timeDelay, anchorConfigs.head.featureAnchor.lateralViewParams)
    }.toList

    swj.FactData(factDF, keys, timeStampExpr, sparkWindowFeatureConfigs)
  }

  /**
   * Get the maximum window duration for all required SWA features from one anchor.
   * The maximum window duration is used for determining how much fact data should be retrieved.
   */
  def getMaxWindowDurationInAnchor(anchorConfig: FeatureAnchorWithSource, requiredFeatures: IndexedSeq[String]): Duration = {
    val timeWindowExtractor = anchorConfig.featureAnchor.extractor.asInstanceOf[TimeWindowConfigurableAnchorExtractor]
    val feathrFeatureConfigs: Map[String, TimeWindowFeatureDefinition] = timeWindowExtractor.features

    val maxDuration = feathrFeatureConfigs
      .filterKeys(requiredFeatures.contains(_))
      .map({
        case (_, featureDef: TimeWindowFeatureDefinition) => featureDef.window
      })
      .max
    log.info(s"Selected max window duration $maxDuration among required features ${feathrFeatureConfigs.keySet.filter(requiredFeatures.contains(_))}")
    maxDuration
  }

  /**
   * Convert a Feathr definition of sliding window aggregation feature to the input of
   * sliding window join library.
   * @param featureName the name of the SWA feature.
   * @param feathrDef the Feathr config for this feature.
   * @param delay the simulated window delay for this feature, loaded from joinConfig settings.
   * @param lateralViewParams lateral view definition
   * @return A SlidingWindowFeature object, used by SWJ library.
   */
  def convertFeathrDefToSwjDef(
      featureName: String,
      feathrDef: TimeWindowFeatureDefinition,
      delay: Duration,
      lateralViewParams: Option[LateralViewParams]): SlidingWindowFeature = {
    val aggType = AggregationType.withName(feathrDef.aggregationType.toString)
    val featureDef = feathrDef.`def`
    val windowSpec = WindowSpec(feathrDef.window, delay) // delay is got from join config
    val groupByCol = feathrDef.groupBy
    val limit = feathrDef.limit
    val groupBySpec = if (groupByCol.isDefined) Some(GroupBySpec(groupByCol.get, limit.getOrElse(0))) else None
    val filter = feathrDef.filter

    val aggregationSpec = aggType match {
      case AggregationType.SUM => new SumAggregate(featureDef)
      case AggregationType.COUNT =>
        // The count aggregation in spark-algorithms repo is implemented as Sum over partial counts.
        // In Feathr's use case, we want to treat the count aggregation as simple count of non-null items.
        val rewrittenDef = s"CASE WHEN ${featureDef} IS NOT NULL THEN 1 ELSE 0 END"
        new CountAggregate(rewrittenDef)
      case AggregationType.COUNT_DISTINCT => new CountDistinctAggregate(featureDef)
      case AggregationType.AVG => new AvgAggregate(featureDef)
      case AggregationType.MAX => new MaxAggregate(featureDef)
      case AggregationType.MIN => new MinAggregate(featureDef)
      case AggregationType.LATEST => new LatestAggregate(featureDef)
      case AggregationType.MAX_POOLING => new MaxPoolingAggregate(featureDef)
      case AggregationType.MIN_POOLING => new MinPoolingAggregate(featureDef)
      case AggregationType.AVG_POOLING => new AvgPoolingAggregate(featureDef)
    }
    swj.SlidingWindowFeature(featureName, aggregationSpec, windowSpec, filter, groupBySpec, lateralViewParams)
  }

  /**
   * Construct a Spark SQL expression for converting string-based timestamp to epoch seconds.
   *
   * There are three possible formats:
   *
   * 1. For timestamp fields already in epoch (seconds) format, the column will be returned as is (casted to long)
   * 2. For timestamp fields in milliseconds since epoch format, the column will be divided by 1000 and returned
   * 3. For date format string, we convert to to UTC timestamp based on the supplied timezone (with a default timezone
   *    of PDT/PST(America/Los_Angeles)). We will then convert to a unix timestamp (seconds since unix epoch)
   *    (https://spark.apache.org/docs/2.1.0/api/sql/index.html#unix_timestamp)
   */
  def constructTimeStampExpr(timeStampCol: String, timeStampFormat: String, timeZone: Option[String] = None): String = {
    if (EPOCH.equalsIgnoreCase(timeStampFormat)) {
      s"""CAST($timeStampCol AS long)"""
    } else if (EPOCH_MILLIS.equalsIgnoreCase(timeStampFormat)) {
      s"""CAST((CAST($timeStampCol AS long) / $MILLIS_IN_SECOND) AS long)"""
    } else {
      // N.B.: This is a simple way to verify that it is actually a valid timestamp format specification
      new SimpleDateFormat(timeStampFormat)
      // Use user specified timezone. If not provided, use the defaultTimeZone(PDT/PST)
      val timestampTimeZone = timeZone.getOrElse(OfflineDateTimeUtils.DEFAULT_TIMEZONE)
      val parsedTimeZone = ZoneId.of(timestampTimeZone).toString

      // 1. Convert to standard timestamp("yyyy-MM-dd HH:mm:ss") format from specified format(timeStampFormat)
      // 2. Convert from other timezone timestamp to UTC timestamp
      // 3. Convert from UTC timestamp to epoch
      // PS: By providing the timezone information through Z patter, to_unix_timestamp will convert the timestamp with
      // specified timezone to epoch
      s"""to_unix_timestamp(
         |  concat(
         |    date_format(
         |      to_utc_timestamp(
         |        to_timestamp($timeStampCol, "$timeStampFormat"),
         |        "$parsedTimeZone"
         |      ),
         |      "$TIMESTAMP_WITHOUT_TIMEZONE_FORMAT"
         |    ),
         |    "$UTC_TIMEZONE_OFFSET"
         |  ),
         |  "$TIMESTAMP_WITH_TIMEZONE_FORMAT"
         |)
         |""".stripMargin
    }
  }

  /**
   * A Spark SQL UDF that applies a bloom filter on a value.
   */
  def mightContain(bf: BloomFilter): UserDefinedFunction = udf((x: String) => if (x != null) bf.mightContain(x) else false)

  /**
   * Convert SWA features returned from spark-algorithms repo to FDS format
   *
   * @param swaRAWDf      raw input dataframe that has SWA features
   * @param allSWAFeatures all SWA feature names
   * @param featureTypeConfigs user specified feature type via feature config
   * @return (FDS formatted DataFrame, inferred feature types)
   */
  def convertSWADFToFDS(swaRAWDf: DataFrame, allSWAFeatures: Set[String], featureNamesToColumnFormat: Map[String, FeatureColumnFormat],
    featureTypeConfigs: Map[String, FeatureTypeConfig]): FeatureDataFrame = {
    val featureColNameToFeatureNameAndType: Map[String, (String, FeatureTypeConfig)] =
      allSWAFeatures.map {
        case featureName =>
          val colName = featureName
          val colType = swaRAWDf.schema.fields(swaRAWDf.schema.fieldIndex(colName)).dataType
          val featureTypeConfig = featureTypeConfigs.getOrElse(featureName, FeatureTypeConfig.UNDEFINED_TYPE_CONFIG)
          val withInferredFeatureTypeConfig = if (featureTypeConfig.getFeatureType == FeatureTypes.UNSPECIFIED) {
            new FeatureTypeConfig(FeaturizedDatasetUtils.inferFeatureTypeFromColumnDataType(colType))
          } else {
            featureTypeConfig
          }
          (colName, (featureName, withInferredFeatureTypeConfig))
      }.toMap

    // If the feature is already in FDS format (egs. -> if custom SQL udf like FDSExtract() is used), it should not be again converted to FDS format.
    val convertedDF = featureColNameToFeatureNameAndType
      .groupBy(pair => featureNamesToColumnFormat(pair._1))
      .foldLeft(swaRAWDf)((inputDF, featureColNameToFeatureNameAndTypeWithFormat) => {
        val fdsDF = featureColNameToFeatureNameAndTypeWithFormat._1 match {
          case FeatureColumnFormat.FDS_TENSOR =>
            inputDF
          case FeatureColumnFormat.RAW =>
            val convertedDF = FeaturizedDatasetUtils.convertRawDFtoQuinceFDS(inputDF, featureColNameToFeatureNameAndType)
            convertedDF
        }
        fdsDF
      })

    val inferredFeatureTypes = featureColNameToFeatureNameAndType.map {
      case (_, (featureName, featureType)) =>
        featureName -> featureType
    }
    offline.FeatureDataFrame(convertedDF, inferredFeatureTypes)
  }

  /**
   * return the [[TimeWindowParams]] for the data source.
   * if it's already defined in the source, just return it; otherwise, return the column created from the source date partition.
   * @param source data source source
   * @return the TimeWindowParams with the column name and the time column format.
   */
  def getTimeWindowParam(source: DataSource): TimeWindowParams = {
    source.timeWindowParams.getOrElse(TimeWindowParams(TIMESTAMP_PARTITION_COLUMN, EPOCH))
  }

  /**
   * check whether we need to create the timestamp column in the data source from the time partition.
   * We need to create it if the source is used in sliding window aggregation and the timeWindowParams is not defined.
   * @param source the datasource source
   * @return true if we need to create the timestamp column.
   */
  def needCreateTimestampColumnFromPartition(source: DataSource): Boolean = {
    val needCreateTimestampColumn = source.timeWindowParams.isEmpty
    if (needCreateTimestampColumn && source.timePartitionPattern.isEmpty) {
      throw new FeathrConfigException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"The source ${source.path} is used in sliding window aggregation, but neither timeWindowParams or timePartitionPattern is defined.")
    }
    needCreateTimestampColumn
  }

  /**
   * Separate the SWA FeatureAnchorWithSources into different groups,
   * based on source key extractor, lateral view, and source.
   * Each group of features will be evaluate on the same DataFrame.
   * @param windowAggAnchorDFThisStage map FeatureAnchorWithSource to DataFrame
   * @return Set of FeatureAnchorWithSource identified by its unique source key extractor, lateral view, and source combination
   */
  def getSWAAnchorGroups(windowAggAnchorDFThisStage: Map[FeatureAnchorWithSource, DataFrame]):
    Seq[Map[FeatureAnchorWithSource, DataFrame]] = {
      windowAggAnchorDFThisStage
      .groupBy { case (anchor: FeatureAnchorWithSource, _) =>
        // If the SWA features are defined on the same source, keyExtractor and lateralView Params,
        // they can be calculated on the same source DataFrame

        // For anchors that have preprocessing, we should not merge them. We simply use feature names of this anchor
        // as a unique identifier to differentiate them.
        val featureNames = PreprocessedDataFrameManager.preprocessedDfMap.nonEmpty match {
          case true => {
            PreprocessedDataFrameManager.getPreprocessingUniquenessForAnchor(anchor)
          }
          case false => ""
        }

        (anchor.featureAnchor.sourceKeyExtractor.toString(), featureNames, anchor.featureAnchor.lateralViewParams, anchor.source)
      }.values.toSeq
  }
}
