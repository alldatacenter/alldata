package com.linkedin.feathr.offline.util

import com.linkedin.feathr.common.{DateParam, JoiningFeatureParams}
import com.linkedin.feathr.offline.anchored.anchorExtractor.{SQLConfigurableAnchorExtractor, TimeWindowConfigurableAnchorExtractor}
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.sparkcommon.{SimpleAnchorExtractorSpark}
import org.apache.logging.log4j.LogManager

import scala.collection.mutable

private[offline] object AnchorUtils {
  type FeatureName = String
  // \w is equal to [a-zA-Z0-9_], it includes _ and excludes -, actually - is ambiguous in Feathr's derived
  // feature context, e.g, if '-' is allowed in feature name, a-b could be a valid feature name or feature a minus feature b
  // feature name cannot start with '_', as this might be reserved for Feathr's internal use
  val featureNamePattern = "^[a-zA-Z][a-zA-Z0-9_]*$".r.pattern
  private val log = LogManager.getLogger(getClass)

  /**
   * remove all non alphaNumeric characters in the input string
   * This function is needed whenever we need to generate a string from key expression/tags with only alphaNumeric
   * characters to be used as a column name in a dataframe.
   * @param input string to process
   */
  def removeNonAlphaNumChars(input: String): String = input.replaceAll("[^\\w]", "")

  /**
   *  Append anchors with corresponding target date time parameters.
   * @param featureName feature name
   * @param dateParams  Map of feature name to DateParam
   * @param allAnchoredFeatures Map of anchor name to [[FeatureAnchorWithSource]]
   * @return   [[FeatureAnchorWithSource]] object
   */
  def getAnchorsWithDate(
      featureName: String,
      dateParams: mutable.HashMap[String, DateParam],
      allAnchoredFeatures: Map[String, FeatureAnchorWithSource]): Option[FeatureAnchorWithSource] = {
    val anchorWithSource = allAnchoredFeatures.get(featureName)
    val dateParam: Option[DateParam] = dateParams.get(featureName)

    def setDate(a: FeatureAnchorWithSource, dateParamOpt: Option[DateParam]): FeatureAnchorWithSource = {
      val dateParam = dateParamOpt match {
        // if all the fields in the date are None, set use None directly
        case Some(date) =>
          if (date.startDate.isEmpty && date.endDate.isEmpty &&
              date.dateOffset.isEmpty && date.numDays.isEmpty) {
            None
          } else {
            Some(date)
          }
        case _ => None
      }
      FeatureAnchorWithSource(a.featureAnchor, a.source, dateParam, Some(Seq(featureName)))
    }

    anchorWithSource match {
      case Some(anchor) => Some(setDate(anchor, dateParam))
      case _ => None
    }
  }

  /**
   * Given a list of string tagged feature names, return a map of feature name to the corresponding data params.
   * @param features  Seq of [[JoiningFeatureParams]]
   * @return  Map of featureName to [[DateParam]]
   */
  def getFeatureDateMap(features: Seq[JoiningFeatureParams]): mutable.HashMap[String, DateParam] = {
    val dateParamsMap = scala.collection.mutable.HashMap.empty[String, DateParam]
    features.foreach(x => dateParamsMap.put(x.featureName, x.dateParam.getOrElse(DateParam())))
    dateParamsMap
  }

  /**
   * A utility method to extract the filter specified in the config.
   *
   * Note: Currently, only [[TimeWindowConfigurableAnchorExtractor]] supports filters.
   * @param anchor
   * @return Some(filter) if Anchor Extractor supports filters else None.
   */
  def getFilterFromAnchor(anchor: FeatureAnchorWithSource, feature: String): Option[String] = {
    anchor.featureAnchor.extractor match {
      case extractor: TimeWindowConfigurableAnchorExtractor => extractor.features(feature).filter
      // The following extractors do not support filter config at the moment. This code should be modified, if and when support is added.
      case _: SQLConfigurableAnchorExtractor => None // Currently this extractor does not support filter config
      case _: SimpleAnchorExtractorSpark => None // Currently this extractor does not support filter config
      case _ => None
    }
  }
}
