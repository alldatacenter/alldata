package com.linkedin.feathr.offline.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.{JsonParser, TreeNode}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.{ArrayNode, ObjectNode, TextNode}
import com.jasonclawson.jackson.dataformat.hocon.HoconFactory
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.common.{DateParam, FeatureRef, FeathrJacksonScalaModule, JoiningFeatureParams}
import com.linkedin.feathr.offline.anchored.WindowTimeUnit
import com.linkedin.feathr.offline.swa.SlidingWindowFeatureUtils
import org.apache.logging.log4j.LogManager

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder, DateTimeParseException}
import java.time.temporal.ChronoField
import java.time.{Duration, LocalDateTime}
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Try

/**
 */
/**
 * Configures the FeatureJoinJob
 *
 *
 */
@JsonDeserialize(using = classOf[FeatureJoinConfigDeserializer])
case class FeatureJoinConfig(groups: Map[String, Seq[KeyedFeatureList]], @JsonProperty("settings") settings: Option[JoinConfigSettings] = None) {

  /*
   * Note that the feature name in JoiningFeatureParams is now a feature reference (and is backward
   * compatible with feature name)
   */
  val featureGroupings: Map[String, Seq[JoiningFeatureParams]] = for {
    (featureGroupName, seqOfKeyedFeatureList) <- groups
    stringTaggedFeatureRefs = seqOfKeyedFeatureList flatMap {
      case KeyedFeatureList(key, featureList, startDate, endDate, dateOffset, numDays, timeDelay, featureAlias) =>
        // For time-based join, either absolute or relative date should be specified.
        val dateParam = if (startDate.isDefined || dateOffset.isDefined) Option(DateParam(startDate, endDate, dateOffset, numDays)) else None
        featureList map (featureRef => JoiningFeatureParams(key, featureRef, dateParam, timeDelay, featureAlias))
    }
  } yield featureGroupName -> stringTaggedFeatureRefs
  val featuresToTimeDelayMap = for {
    (_, seqOfKeyedFeatureList) <- groups
    KeyedFeatureList(_, featureList, _, _, _, _, timeDelay, featureAlias) <- seqOfKeyedFeatureList
    featureRef <- featureList if timeDelay.isDefined
  } yield if (featureAlias.isDefined) featureAlias.get -> timeDelay.get else featureRef -> timeDelay.get

  // includes pass-through features if they're specified in join config
  val joinFeatures: Seq[JoiningFeatureParams] = featureGroupings.values.toSeq.flatten.distinct
}

case class KeyedFeatureList(
    @JsonProperty("key") key: Seq[String],
    @JsonProperty("featureList") featureList: Seq[String],
    @JsonProperty("startDate") startDate: Option[String] = None,
    @JsonProperty("endDate") endDate: Option[String] = None,
    @JsonProperty("dateOffset") dateOffset: Option[String] = None,
    @JsonProperty("numDays") numDays: Option[String] = None,
    @JsonProperty("overrideTimeDelay") overrideTimeDelay: Option[String] = None,
    @JsonProperty("featureAlias") featureAlias: Option[String] = None)

object KeyedFeatureList {

  /**
   * Auxiliary constructor to create KeyedFeatureList without any temporal parameters
   * @param key keys
   * @param featureList Feature names or references or Id strings
   * @return KeyedFeatureList
   */
  def apply(key: Seq[String], featureList: Seq[String]): KeyedFeatureList = {
    new KeyedFeatureList(key, featureList)
  }

  /**
   * Auxiliary constructor to create KeyedFeatureList when startDate and endDate are provided
   * @param key keys
   * @param featureList Feature names or references or Id strings
   * @param startDate start date
   * @param endDate end date
   * @return KeyedFeatureList
   */
  def apply(key: Seq[String], featureList: Seq[String], startDate: String, endDate: String): KeyedFeatureList = {
    val featureRefStrings = buildFeatureRefStrings(featureList)
    KeyedFeatureList(key, featureRefStrings, Some(startDate), Some(endDate))
  }

  /**
   * Auxiliary constructor to create KeyedFeatureList when dateOffset and numDays are provided
   * @param key keys
   * @param featureList Feature names or references or Id strings
   * @param dateOffset date offset
   * @param numDays number of days from the date offset
   * @return KeyedFeatureList
   */
  def apply(key: Seq[String], featureList: Seq[String], dateOffset: Int, numDays: Int): KeyedFeatureList = {
    require(dateOffset > 0, s"Expected dateOffset > 0, found $dateOffset")
    require(numDays > 0, s"Expected numDays > 0, found $numDays")

    val featureRefStrings = buildFeatureRefStrings(featureList)
    KeyedFeatureList(key, featureRefStrings, None, None, Some(dateOffset.toString), Some(numDays.toString))
  }

  private def buildFeatureRefStrings(featureList: Seq[String]): Seq[String] = featureList map { feature =>
    new FeatureRef(feature).toString
  }
}

/*
 * Feature config settings. It can be time aware join or swa. However, we dont support both of them.
 * @param timeWindowJoinSetting        time window join setting definition for swa
 * @param observationTimeConfigSettingDefinition time observation info for time aware join
 */
case class FeatureJoinConfigSettings(
    timeWindowJoinSetting: Option[TimeWindowJoinConfigSettingDefinition],
    observationTimeConfigSettingDefinition: Option[ObsTimeConfigSetting] = None) {
  require(
    timeWindowJoinSetting.isEmpty || observationTimeConfigSettingDefinition.isEmpty,
    s"Only one of time_window_join or observationTimeInfo should be specified but not both")
}

/**
 * ObsTimeConfigSetting Definition class to support time aware join by using the directory.
 * For example, if we have a timestamp field in our observation data, and we want to join with day/hour corresponding to
 * that timestamp.
 *
 * @param timestamp        name of the timestamp column in the observation data
 * @param timestampFormat  he timepath format can be a combination of yyyy, mm and dd in any order, separated by a '-' or '/'. For example,
 *                         it can dd-mm-yyyy or mm-dd-yyyy or yyyy/dd/mm.
 * @param isDaily          Boolean value if set to true defines the resolution of the data. It could be daily or hourly
 * @param simulateTimeDelay     simulate time delay to backdate the joining of the feature data by that duration. Example, if this field is set
 *                                to 7d, the feature data gets backdated to 7 days from what is present in the obs time column.
 * @param useLatestFeatureData  Boolean value, if this is set to true we will always match the observation data with the latest timestamp in our feature
 *                              data.
 */
private[feathr] case class ObsTimeConfigSetting(
    timestamp: String,
    timestampFormat: String,
    isDaily: Boolean, // daily or hourly
    simulateTimeDelay: Option[Duration],
    useLatestFeatureData: Option[Boolean])

@JsonDeserialize(using = classOf[TimeWindowJoinConfigSettingDefinitionDeserializer])
case class TimeWindowJoinConfigSettingDefinition(
    timestamp: String,
    timestamp_format: String,
    dateTimeRange: Option[DateTimeRange],
    simulate_time_delay: Map[String, Duration],
    useLatestFeatureData: Option[Boolean] = None, // if true, ignore the other fields
    localTestJoinJob: Boolean = false)

class TimeWindowJoinConfigSettingDefinitionDeserializer extends JsonDeserializer[TimeWindowJoinConfigSettingDefinition] {
  val TIMESTAMP_FIELD = "timestamp"
  val TIMESTAMP_FORMAT = "timestamp_format"
  val START_TIME = "start_time"
  val END_TIME = "end_time"
  val SIMULATE_TIME_DELAY = "simulate_time_delay"
  val USE_LATEST_FEATURE_DATA = "useLatestFeatureData"
  val DATE_TIME_RANGE_FORMAT = "yyyy/MM/dd/HH/mm/ss"
  val FEATURES_OVERRIDE = "featuresOverride"
  val FEATURES = "features"

  private val log = LogManager.getLogger(getClass)

  private def parseFeaturesOverride(arrayNode: ArrayNode, featuresToTimeDelayMap: mutable.Map[String, Duration]): Map[String, Duration] = {
    val x = arrayNode.elements.asScala.foreach(node => {
      val simulateTimeDelay = node.get(SIMULATE_TIME_DELAY) match {
        case field: TextNode => WindowTimeUnit.parseWindowTime(field.textValue())
        case _ => throw new IllegalArgumentException("The features override section must have a simulate_time_delay field.")
      }
      val features = node.get(FEATURES) match {
        case x: ArrayNode => x.elements.asScala.map(_.asText).toSet.toSeq
        case _ => throw new IllegalArgumentException("The features override section must have a features field.")
      }
      features map (feature => featuresToTimeDelayMap.put(feature, simulateTimeDelay))
    })
    featuresToTimeDelayMap.map(kv => (kv._1, kv._2)).toMap
  }

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): TimeWindowJoinConfigSettingDefinition = {
    val codec = p.getCodec
    val node = codec.readTree[TreeNode](p)

    node match {
      case x: ObjectNode =>
        val timeWindowJoinConfigSettings = if (ConfigLoaderUtils.getBoolean(x, USE_LATEST_FEATURE_DATA)) {
          // If useLatestFeatureData flag is set, we will use current timestamp to load feature data,
          // assume this is for scoring, and all the feature data are available already
          getLatestTimeWindowJoinConfigSettingDefinition(x)
        } else {
          val timeStamp = node.get(TIMESTAMP_FIELD) match {
            case field: TextNode => field.textValue()
            case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"$TIMESTAMP_FIELD field is not correctly set in $node")
          }
          val timeStampFormat =
            node.get(TIMESTAMP_FORMAT) match {
              case field: TextNode => field.textValue()
              case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"$TIMESTAMP_FORMAT field is not correctly set in $node")
            }

          // Parse start_time and end_time to DateTimeRange
          val startTimeOption = parseDateTime(node, START_TIME, timeStampFormat)
          val endTimeOption = parseDateTime(node, END_TIME, timeStampFormat)
          val dateTimeRangeOption = (startTimeOption, endTimeOption) match {
            case (Some(startTime), Some(endTime)) => Some(DateTimeRange(startTime, endTime))
            case (None, None) => None
            case (_, None) => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"$END_TIME is not provided in $node.")
            case (None, _) => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"$START_TIME is not provided in $node")
          }
          val featuresToTimeDelayMap = mutable.Map.empty[String, Duration]
          node.get(FEATURES_OVERRIDE) match {
            case field: ArrayNode => parseFeaturesOverride(field, featuresToTimeDelayMap)
            case _ => None
          }

          node.get(SIMULATE_TIME_DELAY) match {
            // This is the global setting, and should be applied to all the features except those specified using featureOverride.
            case field: TextNode => featuresToTimeDelayMap.put(SlidingWindowFeatureUtils.DEFAULT_TIME_DELAY, WindowTimeUnit.parseWindowTime(field.textValue()))
            case _ => None
          }

          val featuresToDelayImmutableMap = featuresToTimeDelayMap.map(kv => (kv._1, kv._2)).toMap

          TimeWindowJoinConfigSettingDefinition(timeStamp, timeStampFormat, dateTimeRangeOption, featuresToDelayImmutableMap)
        }
        if (node.get("localTest") == null) {
          timeWindowJoinConfigSettings
        } else {
          timeWindowJoinConfigSettings.copy(localTestJoinJob = true)
        }
    }
  }

  /**
   * create a [[TimeWindowJoinConfigSettingDefinition]] with lastest/current timestamp
   * @param node
   * @return
   */
  def getLatestTimeWindowJoinConfigSettingDefinition(node: ObjectNode): TimeWindowJoinConfigSettingDefinition = {
    if (node.has(TIMESTAMP_FIELD) || node.has(TIMESTAMP_FORMAT) || node.has(START_TIME)
        || node.has(END_TIME) || node.has(SIMULATE_TIME_DELAY)) {
      throw new FeathrConfigException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"timestamp related fields($TIMESTAMP_FIELD, $TIMESTAMP_FORMAT, " +
          s"$START_TIME, $END_TIME, $SIMULATE_TIME_DELAY) cannot be used when $USE_LATEST_FEATURE_DATA is set to true.")
    }
    val now = LocalDateTime.now()
    val featuresToTimeDelayMap = Map.empty[String, Duration]
    // Set both start_time and end_time to 'now', these two parameters will be passed into the below function:
    // DataSourceUtils.getWindowAggAnchorDF(ss, source.path, obsDataStartTime, obsDataEndTime, maxDurationPerSource)
    // The actual observation data loaded will be between [start_time - maxWindowSize, end_time],
    // since feature data timestamp is current time, and observation record happened before (now - maxWindowSize)
    // will match any feature data
    TimeWindowJoinConfigSettingDefinition("", "", Some(DateTimeRange(now, now)), featuresToTimeDelayMap, Some(true))
  }

  private def parseDateTime(node: TreeNode, field: String, timeStampFormat: String): Option[LocalDateTime] = {
    node.get(field) match {
      case field: TextNode =>
        // try to parse with provided timestamp. If failed, use timestamp_format to parse since the join config
        // may still use the older Feathr version.
        Try(Some(LocalDateTime.parse(field.textValue(), DateTimeFormatter.ofPattern(DATE_TIME_RANGE_FORMAT)))).recover {
          case _: DateTimeParseException =>
            log.warn(s"Expected timestamp format is: $DATE_TIME_RANGE_FORMAT. But found: $field")
            val dateFormatter = new DateTimeFormatterBuilder()
              .appendPattern(timeStampFormat)
              .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
              .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
              .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
              .toFormatter()
            Some(LocalDateTime.parse(field.textValue(), dateFormatter))
        }.get
      case _ => None
    }
  }
}

case class DateTimeRange(startTime: LocalDateTime, endTime: LocalDateTime)

/**
 * Join config settings, consists of [[ObservationDataTimeSettings]] class, and [[JoinTimeSettings]] class (both optional).
 * @param observationDataTimeSetting  Settings which have parameters specifying how to load the observation data.
 * @param joinTimeSetting Settings which have parameters specifying how to join the observation data with the feature data.
 * @param conflictsAutoCorrectionSetting Settings which have parameters specifying how to solve feature names conflicts with dataset automatically
 */
private[feathr] case class JoinConfigSettings(observationDataTimeSetting: Option[ObservationDataTimeSetting], joinTimeSetting: Option[JoinTimeSetting], conflictsAutoCorrectionSetting: Option[ConflictsAutoCorrectionSetting]) {}

/**
 * ObservationDataTimeSetting Definition class with parameters required to load time partitioned observation data used for the feature join.
 * For example, the user may want to load only the last 3 day's of observation data or may want to load observation data of 09/18/2020 - 09/19/2020.
 *
 * @param dateParam [[DateParam]] object. Extract the absolute time range or relative time range object into a dateParam object.
 * @param timeFormat  An optional timeFormat string. This should be ideally be present if startTime and endTime is specified.
 *                    Otherwise, the format would be assumed to be the SlidingWindowFeaturesUtils.DEFAULT_TIME_FORMAT.
 */
@JsonDeserialize(using = classOf[ObservationDataTimeSettingDefinitionDeserializer])
private[offline] case class ObservationDataTimeSetting(dateParam: DateParam, timeFormat: Option[String] = None)

/**
 * JoinTimeSettings object. This object contains parameters related to the joining of the observation data with the feature data.
 * @param timestampColumn [[timestampColumn]] object
 * @param simulateTimeDelay Amount by which the timestamp column in the observation data should be delayed before joining
 *                          with the feature data. Format supported is Duration.
 * @param useLatestFeatureData  If set to true, none of the other fields should be set. Will use the latest available feature data
 *                              to perform the join.
 */
@JsonDeserialize(using = classOf[JoinTimeConfigSettingDefinitionDeserializer])
private[offline] case class JoinTimeSetting(timestampColumn: TimestampColumn, simulateTimeDelay: Option[Duration], useLatestFeatureData: Boolean)

/**
 * ConflictsAutoCorrectionSetting object. This object contains parameters related to auto correct name conflicts among feature names and dataset.
 * @param renameFeatureList If rename feature list. 'False' by default which means to rename dataset
 * @param suffix            Suffix used to rename conflicted names
 */
@JsonDeserialize(using = classOf[ConflictsAutoCorrectionSettingDeserializer])
private[offline] case class ConflictsAutoCorrectionSetting(renameFeatureList: Boolean, suffix: String)

/**
 * Timestamp column object
 * @param name  Name of the timestamp column, can be sql expression.
 * @param format  Format of the timestamp column
 */
private[offline] case class TimestampColumn(name: String, format: String)

object FeatureJoinConfig {

  /**
   * Parse join configuration content.
   *
   * @param joinConfString join config content
   * @return feature-join.md object
   */
  def parseJoinConfig(joinConfString: String): FeatureJoinConfig = {
    val jackson: ObjectMapper = new ObjectMapper(new HoconFactory)
      .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
      .registerModule(FeathrJacksonScalaModule) // DefaultScalaModule causes a fail on holdem
    jackson.readValue[FeatureJoinConfig](joinConfString, classOf[FeatureJoinConfig])
  }
}
