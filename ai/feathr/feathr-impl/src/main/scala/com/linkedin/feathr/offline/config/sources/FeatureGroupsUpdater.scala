package com.linkedin.feathr.offline.config.sources

import com.linkedin.feathr.common.JoiningFeatureParams
import com.linkedin.feathr.offline.{FeatureDataFrame, JoinKeys}
import com.linkedin.feathr.offline.anchored.anchorExtractor.TimeWindowConfigurableAnchorExtractor
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.offline.job.FeatureJoinJob.{FeatureName, log}
import com.linkedin.feathr.offline.logical.{FeatureGroups, MultiStageJoinPlan}

/**
 * Feature groups will be generated using only the feature def config using the [[com.linkedin.feathr.offline.config.FeatureGroupsGenerator]]
 * class. After a join config is presented in FeathrClient.join method, we may need to update these feature groups to ensure that
 * all the requested features have the right join parameters.
 *
 * For example, the same feature definition can be requested with different time delays.
 */
private[offline] class FeatureGroupsUpdater {
  /**
   * This method updates the feature groups after parsing the join config. It updates the feature groups in the following cases:-
   *
   * a. This method injects any window agg features aliased with a different name into the allAnchoredFeatures map
   * and allWindowAggFeatures map. Since, the feature def config mentions this feature only once, we need to inject aliased name into
   * the feature groups so that the aliased feature gets processed like a new feature.
   *
   * b. Update the featureAnchorWithSource object value for features with DateParams defined.
   * @param featureGroups The original [[FeatureGroups]]
   * @param joiningFeatureParams Map from the feature ref string to the feature alias.
   * @return
   */
  private[offline] def updateFeatureGroups(featureGroups: FeatureGroups, joiningFeatureParams: Seq[JoiningFeatureParams]): FeatureGroups = {

    // Calculate the new entries which are to be added to both the window agg features and anchored features map
    val updatedMapForWindowAggFeatures = joiningFeatureParams.flatMap (
      joiningFeature => {
        val featureName = joiningFeature.featureName
        if (joiningFeature.featureAlias.isDefined) {
          // We need to insert into the feature groups only if it is window agg feature and a time delay is defined.
          // Otherwise we do not need to update the feature groups. We will rename them with the feature alias at a later stage.
          if (featureGroups.allWindowAggFeatures.contains(featureName) && joiningFeature.timeDelay.isDefined) {
            val featureAlias = joiningFeature.featureAlias.get
            val originalFeatureAnchorSource = featureGroups.allAnchoredFeatures(featureName)

            // The extractor for this feature aliased feature needs to be updated with the feature alias name.
            val originalExtractor = originalFeatureAnchorSource.featureAnchor.extractor.asInstanceOf[TimeWindowConfigurableAnchorExtractor]
            val updatedExtractor = new TimeWindowConfigurableAnchorExtractor(Map(featureAlias -> originalExtractor.features(featureName)))
            val updatedFeatureAnchor = originalFeatureAnchorSource.featureAnchor.copy(features = Set(featureAlias),
              extractor = updatedExtractor)
            val updatedFeatureAnchorWithSource = originalFeatureAnchorSource.copy(featureAnchor = updatedFeatureAnchor,
              selectedFeatureNames = Option(Seq(featureAlias)))

            // Add the feature alias name with the updated feature anchor source to the maps.
            Some(featureAlias -> updatedFeatureAnchorWithSource)
          } else None
        } else None
      }).toMap

    // Updated anchored features with the date params object.
    val updatedMapWithDateParams = joiningFeatureParams.flatMap (
      joiningFeature => {
        if (joiningFeature.dateParam.isDefined) {
          val featureName = joiningFeature.featureName
          val featureRefToAnchor = featureGroups.allAnchoredFeatures(featureName)
          val updatedAnchorWithSource = featureRefToAnchor.copy(dateParam = joiningFeature.dateParam, selectedFeatureNames = Some(Seq(featureName)))
          Some(featureName -> updatedAnchorWithSource)
        } else None
      }).toMap

    // Add the above values to the original map
    val updatedAnchoredFeaturesMap = featureGroups.allAnchoredFeatures ++ updatedMapForWindowAggFeatures ++ updatedMapWithDateParams
    val updatedWindowAggFeaturesMap = featureGroups.allWindowAggFeatures ++ updatedMapForWindowAggFeatures

    FeatureGroups(updatedAnchoredFeaturesMap, featureGroups.allDerivedFeatures, updatedWindowAggFeaturesMap, featureGroups.allPassthroughFeatures,
      featureGroups.allSeqJoinFeatures)
  }

  /**
   * Update the feature groups (for Feature gen) based on feature missing features. Few anchored features can be missing if the feature data
   * is not present. Remove those anchored features, and also the corresponding derived feature which are dependent on it.
   * @param featureGroups
   * @param allStageFeatures
   * @param keyTaggedFeatures
   * @return
   */
  def getUpdatedFeatureGroups(featureGroups: FeatureGroups, allStageFeatures: Map[FeatureName, (FeatureDataFrame, JoinKeys)],
    keyTaggedFeatures: Seq[JoiningFeatureParams]): (FeatureGroups, Seq[JoiningFeatureParams]) = {
    val updatedAnchoredFeatures = featureGroups.allAnchoredFeatures.filter(featureRow => allStageFeatures.contains(featureRow._1))
    // Iterate over the derived features and remove the derived features which contains these anchored features.
    val updatedDerivedFeatures = featureGroups.allDerivedFeatures.filter(derivedFeature => {
      // Find the constituent anchored features for every derived feature
      val allAnchoredFeaturesInDerived = derivedFeature._2.consumedFeatureNames.map(_.getFeatureName)
      val containsFeature: Seq[Boolean] = allAnchoredFeaturesInDerived.map(feature => updatedAnchoredFeatures.contains(feature))
      !containsFeature.contains(false)
    })
    val updatedSeqJoinFeature = featureGroups.allSeqJoinFeatures.filter(seqJoinFeature => {
      // Find the constituent anchored features for every derived feature
      val allAnchoredFeaturesInDerived = seqJoinFeature._2.consumedFeatureNames.map(_.getFeatureName)
      val containsFeature: Seq[Boolean] = allAnchoredFeaturesInDerived.map(feature => updatedAnchoredFeatures.contains(feature))
      !containsFeature.contains(false)
    })
    val updatedWindowAggFeatures = featureGroups.allWindowAggFeatures.filter(windowAggFeature => updatedAnchoredFeatures.contains(windowAggFeature._1))

    log.warn(s"Removed the following features:- ${featureGroups.allAnchoredFeatures.keySet.diff(updatedAnchoredFeatures.keySet)}," +
      s"${featureGroups.allDerivedFeatures.keySet.diff(updatedDerivedFeatures.keySet)}," +
      s" ${featureGroups.allSeqJoinFeatures.keySet.diff(updatedSeqJoinFeature.keySet)}")
    val updatedFeatureGroups = FeatureGroups(updatedAnchoredFeatures, updatedDerivedFeatures, updatedWindowAggFeatures,
      featureGroups.allPassthroughFeatures, updatedSeqJoinFeature)
    val updatedKeyTaggedFeatures = keyTaggedFeatures.filter(feature => updatedAnchoredFeatures.contains(feature.featureName)
      || updatedDerivedFeatures.contains(feature.featureName) || updatedWindowAggFeatures.contains(feature.featureName)
      || featureGroups.allPassthroughFeatures.contains(feature.featureName) || updatedSeqJoinFeature.contains(feature.featureName))
    (updatedFeatureGroups, updatedKeyTaggedFeatures)
  }

  /**
   * Update the feature groups (for Feature join) based on feature missing features. Few anchored features can be missing if the feature data
   * is not present. Remove those anchored features, and also the corresponding derived feature which are dependent on it.
   *
   * @param featureGroups
   * @param allStageFeatures
   * @param keyTaggedFeatures
   * @return
   */
  def removeMissingFeatures(featureGroups: FeatureGroups, allAnchoredFeaturesWithData: Seq[String],
    keyTaggedFeatures: Seq[JoiningFeatureParams]): (FeatureGroups, Seq[JoiningFeatureParams]) = {

    // We need to add the window agg features to it as they are also considered anchored features.
    val updatedAnchoredFeatures = featureGroups.allAnchoredFeatures.filter(featureRow =>
      allAnchoredFeaturesWithData.contains(featureRow._1)) ++ featureGroups.allWindowAggFeatures ++ featureGroups.allPassthroughFeatures

    val updatedSeqJoinFeature = featureGroups.allSeqJoinFeatures.filter(seqJoinFeature => {
      // Find the constituent anchored features for every derived feature
      val allAnchoredFeaturesInDerived = seqJoinFeature._2.consumedFeatureNames.map(_.getFeatureName)
      val containsFeature: Seq[Boolean] = allAnchoredFeaturesInDerived.map(feature => updatedAnchoredFeatures.contains(feature))
      !containsFeature.contains(false)
    })

    // Iterate over the derived features and remove the derived features which contains these anchored features.
    val updatedDerivedFeatures = featureGroups.allDerivedFeatures.filter(derivedFeature => {
      // Find the constituent anchored features for every derived feature
      val allAnchoredFeaturesInDerived = derivedFeature._2.consumedFeatureNames.map(_.getFeatureName)
      val containsFeature: Seq[Boolean] = allAnchoredFeaturesInDerived.map(feature => updatedAnchoredFeatures.contains(feature)
        || featureGroups.allDerivedFeatures.contains(feature))
      !containsFeature.contains(false)
    }) ++ updatedSeqJoinFeature

    log.warn(s"Removed the following features:- ${featureGroups.allAnchoredFeatures.keySet.diff(updatedAnchoredFeatures.keySet)}," +
      s"${featureGroups.allDerivedFeatures.keySet.diff(updatedDerivedFeatures.keySet)}," +
      s" ${featureGroups.allSeqJoinFeatures.keySet.diff(updatedSeqJoinFeature.keySet)}")
    val updatedFeatureGroups = FeatureGroups(updatedAnchoredFeatures, updatedDerivedFeatures, featureGroups.allWindowAggFeatures,
      featureGroups.allPassthroughFeatures, updatedSeqJoinFeature)
    val updatedKeyTaggedFeatures = keyTaggedFeatures.filter(feature => updatedAnchoredFeatures.contains(feature.featureName)
      || updatedDerivedFeatures.contains(feature.featureName) || featureGroups.allWindowAggFeatures.contains(feature.featureName)
      || featureGroups.allPassthroughFeatures.contains(feature.featureName) || updatedSeqJoinFeature.contains(feature.featureName))
    (updatedFeatureGroups, updatedKeyTaggedFeatures)
  }

  /**
   * Exclude anchored and derived features features from the join stage if they do not have a valid path.
   * @param featureToPathsMap Map of anchored feature names to their paths
   * @param featureGroups All feature groups
   * @param invalidPaths List of all invalid paths
   * @return
   */
  def getUpdatedFeatureGroupsWithoutInvalidPaths(featureToPathsMap: Map[String, String], featureGroups: FeatureGroups, invalidPaths: Seq[String]): FeatureGroups = {
    val updatedAnchoredFeatures = featureGroups.allAnchoredFeatures.filter(featureNameToAnchoredObject => {
      !invalidPaths.contains(featureToPathsMap(featureNameToAnchoredObject._1))
    })

    // Iterate over the derived features and remove the derived features which contains these anchored features.
    val updatedDerivedFeatures = featureGroups.allDerivedFeatures.filter(derivedFeature => {
      // Find the constituent anchored features for every derived feature
      val allAnchoredFeaturesInDerived = derivedFeature._2.consumedFeatureNames.map(_.getFeatureName)
      // Check if any of the features does not have a valid path
      val containsFeature: Seq[Boolean] = allAnchoredFeaturesInDerived
      .map(featureName => !invalidPaths.contains(featureToPathsMap(featureName)))
      !containsFeature.contains(false)
    })

    // Iterate over the seq join features and remove the derived features which contains these anchored features.
    val updatedSeqJoinFeatures = featureGroups.allSeqJoinFeatures.filter(seqJoinFeature => {
      // Find the constituent anchored features for every derived feature
      val allAnchoredFeaturesInDerived = seqJoinFeature._2.consumedFeatureNames.map(_.getFeatureName)
      // Check if any of the features does not have a valid path
      val containsFeature: Seq[Boolean] = allAnchoredFeaturesInDerived
        .map(featureName => !invalidPaths.contains(featureToPathsMap(featureName)))
      !containsFeature.contains(false)
    })

    log.warn(s"Removed the following features:- ${featureGroups.allAnchoredFeatures.keySet.diff(updatedAnchoredFeatures.keySet)}," +
      s"${featureGroups.allDerivedFeatures.keySet.diff(updatedDerivedFeatures.keySet)}," +
      s" ${featureGroups.allSeqJoinFeatures.keySet.diff(updatedSeqJoinFeatures.keySet)}")
    FeatureGroups(updatedAnchoredFeatures, updatedDerivedFeatures,
      featureGroups.allWindowAggFeatures, featureGroups.allPassthroughFeatures, updatedSeqJoinFeatures)
  }

}

/**
 * Companion object for FeatureGroupsUpdater.
 */
private[offline] object FeatureGroupsUpdater {
  def apply(): FeatureGroupsUpdater = new FeatureGroupsUpdater
}

