package com.linkedin.feathr.offline.anchored.feature

import com.linkedin.feathr.common
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.common.{DateParam, FeatureTypeConfig, FeatureValue}
import com.linkedin.feathr.offline.source.{DataSource, SourceFormatType}
import com.linkedin.feathr.offline.util.SourceUtils

/**
 *  A wrapper class to contain information of anchor and source
 *
 *  _________            __________
 * |         | 1*     1 |          |
 * | Feature |__________| Source   |
 * |_________|     .    |__________|
 *                 .
 *             ____.___________
 *            |                |
 *            | Anchor         |
 *            |1. keyExtractor |
 *            |2. Time         |
 *            |3. ...          |
 *            |________________|
 *
 *
 */
private[offline] case class FeatureAnchorWithSource(
    featureAnchor: FeatureAnchor, // anchor definition
    source: DataSource, // data source
    dateParam: Option[DateParam], // date parameter for non-sliding window aggregation features
    // set to private, as we want to treat None as select all features, user should use selectedFeatures instead
    private val selectedFeatureNames: Option[Seq[String]]) {
  // all selected features in this anchor with source, this is needed because user can just request a subset of all
  // features defined in an anchor, this also means we may have multiple FeatureAnchorWithSource with different selectedFeatures
  // corresponding to the same anchor
  val selectedFeatures: Seq[String] = selectedFeatureNames.getOrElse(featureAnchor.getProvidedFeatureNames)

  override def toString: String =
    "anchor: " + featureAnchor.toString + ", source: " + source.toString +
      ", dataParam" + dateParam.toString + ", selected features: " + selectedFeatures.mkString(",")
}

private[offline] object FeatureAnchorWithSource {

  /**
   * build a feature anchor with source
   * @param featureAnchor feature anchor
   * @param dataSourceMap source identifier to data srouce map
   * @param dateParam date parameters of the anchor
   * @param selectedFeatures selected features of this anchor
   * @return a FeatureAnchorWithSource
   */
  def apply(
      featureAnchor: FeatureAnchor,
      dataSourceMap: Option[Map[String, DataSource]] = None,
      dateParam: Option[DateParam] = None,
      selectedFeatures: Option[Seq[String]] = None): FeatureAnchorWithSource = {
    val dataSource = getSource(featureAnchor, dataSourceMap)
    new FeatureAnchorWithSource(featureAnchor, dataSource, dateParam, selectedFeatures)
  }

  // Each feature anchor carries a sourceIdentifier
  // the Feature Anchor can be used only after setting the source using the source section information (passed as a Map)
  // if there is no sources implemented, should be able to pass a None here
  private def getSource(featureAnchor: FeatureAnchor, sourceMap: Option[Map[String, DataSource]] = None): DataSource = {
    val sourceIdentifier = featureAnchor.sourceIdentifier
    if (SourceUtils.isFilePath(sourceIdentifier)) {
      // .csv and .json are for unit test only
      // sourceIdentifier can still be an HDFS path
      DataSource(sourceIdentifier, SourceFormatType.FIXED_PATH, None, None)
    } else if (sourceIdentifier.equals("PASSTHROUGH")) {
      DataSource("PASSTHROUGH", SourceFormatType.FIXED_PATH, None, None)
    } else {
      // if sourceIdentifier is not a file path, there must be a "sources" section in the config
      val sourceMapToUse = sourceMap match {
        case Some(v) => v
        case None => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, "Source is not defined.")
      }

      val dataSource = sourceMapToUse.get(sourceIdentifier) match {
        case Some(v) => v
        case None => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"Source is not defined in source section $sourceMapToUse")
      }
      dataSource
    }
  }

  /**
   * Helper method to get default values specified for anchored features.
   * This method checks the feature definition config to see if defaults have been specified.
   * @param anchoredFeatures anchored features (parsed from feature definition).
   * @return a map of features to its default value as FeatureValue.
   */
  def getDefaultValues(anchoredFeatures: Seq[FeatureAnchorWithSource]): Map[String, FeatureValue] = {
    anchoredFeatures.map(_.featureAnchor.defaults).foldLeft(Map.empty[String, common.FeatureValue])(_ ++ _)
  }

  /**
   * Helper method to get feature types defined in anchor definition, if any.
   * @param anchoredFeatures anchored features (parsed from feature definition).
   * @return a map of features to its type (as FeatureTypes).
   */
  def getFeatureTypes(anchoredFeatures: Seq[FeatureAnchorWithSource]): Map[String, FeatureTypeConfig] = {
    anchoredFeatures
      .map(x => Some(x.featureAnchor.featureTypeConfigs))
      .foldLeft(Map.empty[String, FeatureTypeConfig])((a, b) => a ++ b.getOrElse(Map.empty[String, FeatureTypeConfig]))
  }
}
