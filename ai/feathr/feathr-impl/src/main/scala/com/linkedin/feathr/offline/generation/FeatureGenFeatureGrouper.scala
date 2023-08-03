package com.linkedin.feathr.offline.generation

import com.linkedin.feathr.common.{FeatureTypeConfig, RichConfig}
import com.linkedin.feathr.common.configObj.generation.OutputProcessorConfig
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException}
import com.linkedin.feathr.offline.{FeatureDataFrame, FeatureDataWithJoinKeys}
import com.linkedin.feathr.offline.derived.DerivedFeature
import com.linkedin.feathr.offline.generation.FeatureGenerationPathName.FEATURES
import com.linkedin.feathr.offline.join.algorithms.JoinType.full_outer
import com.linkedin.feathr.offline.join.algorithms.{SparkJoin, SparkJoinWithNoJoinCondition}

/**
 * Parses output processor configs and joins specified features on to a single DataFrame.
 * It is possible that the features may be loaded from source on to different DataFrame.
 * After this step, the will be joined / grouped on to a single DataFrame.
 */
private[offline] class FeatureGenFeatureGrouper private[generation] () {

  /**
   * Groups input features based on the grouping specified in output processor
   * @param featureData            Generated anchored feature data.
   * @param outputProcessorConfigs Feature generation output processor configs.
   * @param allDerivedFeatures     Map of all derived features
   * @param joiner                 The joiner algorithm to join different features to a single DataFrame.
   * @return
   */
  def group(
      featureData: FeatureDataWithJoinKeys,
      outputProcessorConfigs: Seq[OutputProcessorConfig],
      allDerivedFeatures: Map[String, DerivedFeature],
      joiner: SparkJoin = SparkJoinWithNoJoinCondition()): FeatureDataWithJoinKeys = {
    outputProcessorConfigs.foldLeft(featureData)((accFeatureData, config) => {
      val featureGroupFieldOpt = config.getParams.getStringListOpt(FEATURES)
      if (featureGroupFieldOpt.isEmpty) {
        accFeatureData
      } else {
        // If the field exists but is empty, then group ALL features together
        val featuresToGroup = featureGroupFieldOpt.getOrElse(accFeatureData.keys.toSeq)
        // Iterate through all features to group and if there are any derived features, add the required anchor features also
        val allFeaturesToGroup = featuresToGroup.flatMap(feature => getRootAnchorFeatures(feature, allDerivedFeatures)).distinct
        val filteredFeatureData = accFeatureData.filter(f => allFeaturesToGroup.contains(f._1))
        if (filteredFeatureData.isEmpty) {
          throw new FeathrException(
            ErrorLabel.FEATHR_USER_ERROR,
            s"Features specified in output processor are missing in generated feature set. Please check feature generation spec. " +
              s" Could not find features: [${featuresToGroup.toSet.diff(accFeatureData.keySet).mkString(",")}]")
        }
        accFeatureData ++ joinGroupedFeatures(filteredFeatureData, joiner)
      }
    })
  }

  /**
   * Given a derived feature name, this helper function returns the list of anchor features that are required to compute this feature.
   * Note we make the assumption that if the feature is not a derived feature then it is an anchor feature. This is valid in
   * the context which this helper function is used.
   * @param feature feature name
   * @param allDerivedFeatures map of all derived features
   * @return
   */
  private def getRootAnchorFeatures(feature: String, allDerivedFeatures: Map[String, DerivedFeature]): Seq[String] = {
    if (!allDerivedFeatures.contains(feature)) {
      List(feature)
    }
    else {
      allDerivedFeatures(feature).consumedFeatureNames.map(f => f.getFeatureName())
        .flatMap(f => getRootAnchorFeatures(f, allDerivedFeatures)).distinct
    }
  }

  /**
   * Joins all input features to single DataFrame. The output should contain a single DataFrame.
   * @param featureData Generated anchored feature data.
   * @param joiner The joiner algorithm to join different features to a single DataFrame.
   * @return
   */
  def joinGroupedFeatures(featureData: FeatureDataWithJoinKeys, joiner: SparkJoin): FeatureDataWithJoinKeys = {
    if (featureData.isEmpty) {
      return featureData
    }
    val featureDataGroupedByDF = featureData
      .groupBy(_._2._1.df) // Group by dataframe
      .toSeq

    // Merge all the FeatureDataFrame
    // This means, we join the different DF and merge the Feature -> FeatureTypeConfig map (inferredFeatureTypeConfig).
    // We will pick the left join key for the joined DF to be the keys of the first DF.
    val leftJoinKeys = featureDataGroupedByDF.head._2.head._2._2

    val mergedFeatureDF = featureDataGroupedByDF.tail.foldLeft(featureDataGroupedByDF.head._1)((accumulatedFeatureDF, currGroupedFeatureDF) => {
      val rightJoinKeys = currGroupedFeatureDF._2.head._2._2 // Pick the keys from the first value since they all share the same key.
      val rightDF = currGroupedFeatureDF._1
      val joinedDF = joiner.join(leftJoinKeys, accumulatedFeatureDF, rightJoinKeys, rightDF, full_outer)
      joinedDF
    })
    // Merge the inferred TypeConfig for all the features joined.
    val mergedInferredTypeConfigs = featureData.foldLeft(Map.empty[String, FeatureTypeConfig])((accTypeConfig, currFeatureData) => {
      accTypeConfig ++ currFeatureData._2._1.inferredFeatureType
    })

    val outputFeatureDataFrame = FeatureDataFrame(mergedFeatureDF, mergedInferredTypeConfigs)
    // Update FeatureDataFrame and Join keys for all features
    featureData.mapValues(_ => (outputFeatureDataFrame, leftJoinKeys))
  }
}

/**
 * Companion object.
 */
private[offline] object FeatureGenFeatureGrouper {

  /**
   * Factory to instantiate FeatureGenFeatureGrouper.
   * @param joiner                 optional parameter. Selects SparkJoinWithNoJoinCondition by default.
   * @return
   */
  def apply(joiner: SparkJoin = SparkJoinWithNoJoinCondition()): FeatureGenFeatureGrouper =
    new FeatureGenFeatureGrouper()
}
