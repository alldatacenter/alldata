package com.linkedin.feathr.offline.config

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.core.{JsonParser, TreeNode}
import com.fasterxml.jackson.databind.node.{NumericNode, ObjectNode, TextNode, TreeTraversingParser, ValueNode}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonNode}
import com.linkedin.feathr.common.DateParam
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.offline.anchored.WindowTimeUnit

import java.time.LocalDateTime
import scala.collection.JavaConverters._

/**
 * Deserializer class for feature join config
 */
private class FeatureJoinConfigDeserializer extends JsonDeserializer[FeatureJoinConfig] {
  type Arg = Map[String, Seq[KeyedFeatureList]]
  private val OBSERVATION_DATA_TIME_SETTINGS = "observationDataTimeSettings"
  private val JOIN_TIME_SETTINGS = "joinTimeSettings"
  private val CONFLICTS_AUTO_CORRECTION_SETTINGS = "conflictsAutoCorrectionSettings"

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): FeatureJoinConfig = {

    val rootNode = jp.readValueAsTree[JsonNode]

    // We read the observationPath and outputPath from the feature_join conf into Spark job via python script
    // so we don't need to use these information here. But to make config parser happy, we have to remove
    // the nodes from the config.
    rootNode.asInstanceOf[ObjectNode].remove("observationPath")
    rootNode.asInstanceOf[ObjectNode].remove("outputPath")

    val settingsNode = rootNode.asInstanceOf[ObjectNode].remove("settings")
    val settingsConfig = settingsNode match {
      case null => None
      case _ => Some(parseFeatureJoinConfigSettings(settingsNode, jp))
    }

    val featuresTreeParser = new TreeTraversingParser(rootNode, jp.getCodec)
    val groupFeatures = featuresTreeParser.getCodec.readValue[Arg](featuresTreeParser, new TypeReference[Arg] {})
    FeatureJoinConfig(groupFeatures, settingsConfig)
  }

  /**
   * Settings node config parser
   * @param settingsNode  join config's settings node
   * @param jp  json parser
   * @return  join config settings object
   */
  private def parseFeatureJoinConfigSettings(settingsNode: JsonNode, jp: JsonParser): JoinConfigSettings = {

    val supportedSettingFields = Set(OBSERVATION_DATA_TIME_SETTINGS, JOIN_TIME_SETTINGS, CONFLICTS_AUTO_CORRECTION_SETTINGS)
    val unrecognizedFields = settingsNode.fieldNames().asScala.toSet -- supportedSettingFields
    if (unrecognizedFields.nonEmpty) {
      throw new FeathrConfigException(
        ErrorLabel.FEATHR_USER_ERROR,
        "unrecognized: " + unrecognizedFields.mkString(" ") + s" in 'settings'. current supported configurations include $supportedSettingFields.")
    }

    val observationDataTimeSettingsDefinition = settingsNode.get(OBSERVATION_DATA_TIME_SETTINGS) match {
      case observationDataTimeSettingNode: ObjectNode =>
        val observationDataTimeConfigTreeParser = new TreeTraversingParser(observationDataTimeSettingNode, jp.getCodec)
        Some(observationDataTimeConfigTreeParser.getCodec.readValue(observationDataTimeConfigTreeParser, classOf[ObservationDataTimeSetting]))
      case _ => None
    }

    val joinTimeSettingsDefinition = settingsNode.get(JOIN_TIME_SETTINGS) match {
      case joinTimeConfigNode: ObjectNode =>
        val joinTimeConfigTreeParser = new TreeTraversingParser(joinTimeConfigNode, jp.getCodec)
        Some(joinTimeConfigTreeParser.getCodec.readValue(joinTimeConfigTreeParser, classOf[JoinTimeSetting]))
      case _ => None
    }

    val conflictsAutoCorrectionSetting = settingsNode.get(CONFLICTS_AUTO_CORRECTION_SETTINGS) match {
      case autoCorrectionNode: ObjectNode =>
        val autoCorrectionTreeParser = new TreeTraversingParser(autoCorrectionNode, jp.getCodec)
        Some(autoCorrectionTreeParser.getCodec.readValue(autoCorrectionTreeParser, classOf[ConflictsAutoCorrectionSetting]))
      case _ => None
    }

    JoinConfigSettings(observationDataTimeSettingsDefinition, joinTimeSettingsDefinition, conflictsAutoCorrectionSetting)
  }
}

/**
 * Deserializer class for observationDataTime config
 */
private class ObservationDataTimeSettingDefinitionDeserializer extends JsonDeserializer[ObservationDataTimeSetting] {
  val RELATIVE_TIME_RANGE = "relativeTimeRange"
  val ABSOLUTE_TIME_RANGE = "absoluteTimeRange"
  val START_TIME = "startTime"
  val END_TIME = "endTime"
  val WINDOW = "window"
  val OFFSET = "offset"
  val TIME_FORMAT = "timeFormat"

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): ObservationDataTimeSetting = {
    val codec = p.getCodec
    val node = codec.readTree[TreeNode](p)

    node match {
      case innerNode: ObjectNode =>
        val relativeTimeRange = node.get(RELATIVE_TIME_RANGE) match {
          case field: ObjectNode =>
            val window = field.get(WINDOW) match {
              case innerField: TextNode => innerField.textValue()
              case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"$WINDOW field is a required field in $field")
            }

            val offset = field.get(OFFSET) match {
              case innerField: TextNode => Some(innerField.textValue())
              case _ => None
            }

            val supportedSettingFields = Set(WINDOW, OFFSET)
            val unrecognizedFields = field.fieldNames().asScala.toSet -- supportedSettingFields
            if (unrecognizedFields.nonEmpty) {
              throw new FeathrConfigException(
                ErrorLabel.FEATHR_USER_ERROR,
                "unrecognized: " + unrecognizedFields.mkString(" ") + s" in 'settings'. current supported configurations include $supportedSettingFields.")
            }
            Some(RelativeTimeRange(window, offset))
          case _ => None
        }
        val absoluteTimeRange = node.get(ABSOLUTE_TIME_RANGE) match {
          case field: ObjectNode =>
            val timeFormat = field.get(TIME_FORMAT) match {
              case field: TextNode => field.textValue()
              case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"$TIME_FORMAT field is a required field in $node")
            }

            val startTime = field.get(START_TIME) match {
              case field: TextNode => field.textValue()
              case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"$START_TIME field is a required field in $node")
            }
            val endTime = field.get(END_TIME) match {
              case field: TextNode => field.textValue()
              case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"$END_TIME field is a required field in $node")
            }

            val supportedSettingFields = Set(TIME_FORMAT, START_TIME, END_TIME)
            val unrecognizedFields = field.fieldNames().asScala.toSet -- supportedSettingFields
            if (unrecognizedFields.nonEmpty) {
              throw new FeathrConfigException(
                ErrorLabel.FEATHR_USER_ERROR,
                "unrecognized: " + unrecognizedFields.mkString(" ") + s" in 'settings'. current supported configurations include $supportedSettingFields.")
            }

            Some(AbsoluteTimeRange(startTime, endTime, timeFormat))
          case _ => None
        }

        if (relativeTimeRange.isDefined && absoluteTimeRange.isDefined) {
          throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"Both $RELATIVE_TIME_RANGE and $ABSOLUTE_TIME_RANGE cannot be set.")
        }

        if (relativeTimeRange.isEmpty && absoluteTimeRange.isEmpty) {
          throw new FeathrConfigException(
            ErrorLabel.FEATHR_USER_ERROR,
            s"$RELATIVE_TIME_RANGE and $ABSOLUTE_TIME_RANGE are not set. If intention is to not" +
              s"restrict the size of the observation data, please remove the observationDataTimeSettings section completely.")
        }

        val supportedSettingFields = Set(ABSOLUTE_TIME_RANGE, RELATIVE_TIME_RANGE)
        val unrecognizedFields = node.fieldNames().asScala.toSet -- supportedSettingFields
        if (unrecognizedFields.nonEmpty) {
          throw new FeathrConfigException(
            ErrorLabel.FEATHR_USER_ERROR,
            "unrecognized: " + unrecognizedFields.mkString(" ") + s" in 'settings'. current supported configurations include $supportedSettingFields.")
        }

        val timeRange = if (relativeTimeRange.isDefined) {
          DateParam(None, None, relativeTimeRange.get.offset, Some(relativeTimeRange.get.window))
        } else if (absoluteTimeRange.isDefined) {
          DateParam(Some(absoluteTimeRange.get.startTime), Some(absoluteTimeRange.get.endTime), None, None)
        } else { // get the latest always
          DateParam(Some(LocalDateTime.now().toString), Some(LocalDateTime.now().toString))
        }

        val timeFormat = absoluteTimeRange match {
          case Some(range) => Some(range.timeFormat)
          case _ => None
        }

        ObservationDataTimeSetting(timeRange, timeFormat)
    }
  }
}

/**
 * JoinTimeSettings config deserializer.
 */
private class JoinTimeConfigSettingDefinitionDeserializer extends JsonDeserializer[JoinTimeSetting] {
  val TIMESTAMP_COLUMN = "timestampColumn"
  val TIMESTAMP_COLUMN_NAME = "def"
  val TIMESTAMP_COLUMN_FORMAT = "format"
  val SIMULATE_TIME_DELAY = "simulateTimeDelay"
  val USE_LATEST_FEATURE_DATA = "useLatestFeatureData"

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): JoinTimeSetting = {
    val codec = p.getCodec
    val node = codec.readTree[TreeNode](p)

    node match {
      case innerNode: ObjectNode =>
        val timeStamp = node.get(TIMESTAMP_COLUMN) match {
          case field: ObjectNode =>
            val timestampColName = field.get(TIMESTAMP_COLUMN_NAME) match {
              case name: TextNode => name.textValue()
              case _ =>
                throw new FeathrConfigException(
                  ErrorLabel.FEATHR_USER_ERROR,
                  s"$TIMESTAMP_COLUMN_NAME field is not " +
                    s"correctly set in $TIMESTAMP_COLUMN node.")
            }
            val timestampColFormat = field.get(TIMESTAMP_COLUMN_FORMAT) match {
              case format: TextNode => format.textValue()
              case _ =>
                throw new FeathrConfigException(
                  ErrorLabel.FEATHR_USER_ERROR,
                  s"$TIMESTAMP_COLUMN_FORMAT field is not " +
                    s"correctly set in $TIMESTAMP_COLUMN node.")
            }

            val supportedSettingFields = Set(TIMESTAMP_COLUMN_NAME, TIMESTAMP_COLUMN_FORMAT)
            val unrecognizedFields = field.fieldNames().asScala.toSet -- supportedSettingFields
            if (unrecognizedFields.nonEmpty) {
              throw new FeathrConfigException(
                ErrorLabel.FEATHR_USER_ERROR,
                "unrecognized: " + unrecognizedFields.mkString(" ") + s" in 'settings'. current supported configurations include $supportedSettingFields.")
            }
            TimestampColumn(timestampColName, timestampColFormat)
          case _ => TimestampColumn("", "")
        }

        val simulateTimeDelay = node.get(SIMULATE_TIME_DELAY) match {
          // This is the global setting, and should be applied to all the features except those specified using timeDelayOverride.
          case field: TextNode => Some(WindowTimeUnit.parseWindowTime(field.textValue()))
          case _ => None
        }

        val useLatestFeatureData = ConfigLoaderUtils.getBoolean(innerNode, USE_LATEST_FEATURE_DATA)

        val supportedSettingFields = Set(USE_LATEST_FEATURE_DATA, TIMESTAMP_COLUMN, SIMULATE_TIME_DELAY)
        val unrecognizedFields = node.fieldNames().asScala.toSet -- supportedSettingFields
        if (unrecognizedFields.nonEmpty) {
          throw new FeathrConfigException(
            ErrorLabel.FEATHR_USER_ERROR,
            "unrecognized: " + unrecognizedFields.mkString(" ") + s" in 'settings'. current supported configurations include $supportedSettingFields.")
        }

        if (useLatestFeatureData) {
          if (timeStamp.name != "" || timeStamp.format != "" || simulateTimeDelay != None) {
            throw new FeathrConfigException(
              ErrorLabel.FEATHR_USER_ERROR,
              "When useLatestFeatureData flag is set to true, there should be no other fields present in the joinTimeSettings section.")
          }
        }

        JoinTimeSetting(timeStamp, simulateTimeDelay, useLatestFeatureData)
    }
  }
}

/**
 * ConflictsAutoCorrectionSetting config deserializer.
 */
private class ConflictsAutoCorrectionSettingDeserializer extends JsonDeserializer[ConflictsAutoCorrectionSetting] {
  val RENAME_FEATURES = "renameFeatures"
  val RENAME_FEATURES_DEFAULT_VALUE = false
  val AUTO_CORRECTION_SUFFIX = "suffix"
  val AUTO_CORRECTION_SUFFIX_VALUE = "1"

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): ConflictsAutoCorrectionSetting = {
    val codec = p.getCodec
    val node = codec.readTree[TreeNode](p)

    node match {
      case innerNode: ObjectNode =>
        val renameFeatures = innerNode.get(RENAME_FEATURES) match {
          case value: TextNode => (value.textValue() == "True")
          case _ => RENAME_FEATURES_DEFAULT_VALUE
        }
        val suffix = innerNode.get(AUTO_CORRECTION_SUFFIX) match {
          case suffix: TextNode => suffix.textValue()
          case suffix: NumericNode => suffix.asInt().toString
          case _ => AUTO_CORRECTION_SUFFIX_VALUE
        }
        ConflictsAutoCorrectionSetting(renameFeatures, suffix)
      case _ => ConflictsAutoCorrectionSetting(RENAME_FEATURES_DEFAULT_VALUE, AUTO_CORRECTION_SUFFIX_VALUE)
    }
  }
}
/**
 * Parameters which relate on how to load the time range relative to current timestamp (reference time).
 * For example, as reference time is LATEST, then we take the job execution timestamp as the reference time and the other fields are relative to this
 * time.
 *
 * @param window number of days/hours from the current timestamp. Has to be >=1.
 * @param offset The number of days/hours to look back relative to the current timestamp. Has to be >=1.
 *               E.g. reference date is 20200811, if dateOffset is 1d, then the endDate of the feature data is 20200810.
 */
private[offline] case class RelativeTimeRange(window: String, offset: Option[String])

/**
 * Parameters which relate on how to load the time range with a fixed start and end time.
 *
 * @param startTime The absolute start time of the data to be loaded. The format should be the same as the timestamp pattern.
 * @param endTime   The absolute end time of the data to be loaded. The format should be the same as the timestamp pattern.
 * @param timeFormat  The format of the timestamps used in the above times.
 */
private[offline] case class AbsoluteTimeRange(startTime: String, endTime: String, timeFormat: String)
