package com.linkedin.feathr.offline.logical

import com.linkedin.feathr.common.FeatureTypeConfig
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.offline.derived.DerivedFeature

/**
 * This class holds all features (with associated metadata) found in the feature definition config(s)
 * provided as input. The class groups these features to different types.
 * FeatureGroups are used in Planning and execution layers.
 * @param allAnchoredFeatures     all anchored features with anchor and source information (includes allWindowAggFeatures and allPassthroughFeatures).
 * @param allDerivedFeatures      all derived features with derivation and dependent features (include sequential join features).
 * @param allWindowAggFeatures    all SWA anchored features with anchor and source information.
 * @param allPassthroughFeatures  all passthrough features, features already present in the observation.
 * @param allSeqJoinFeatures      all sequential join features.
 */
private[offline] case class FeatureGroups(
    allAnchoredFeatures: Map[String, FeatureAnchorWithSource],
    allDerivedFeatures: Map[String, DerivedFeature],
    allWindowAggFeatures: Map[String, FeatureAnchorWithSource],
    allPassthroughFeatures: Map[String, FeatureAnchorWithSource],
    allSeqJoinFeatures: Map[String, DerivedFeature]) {

    val allTypeConfigs: Map[String, FeatureTypeConfig] = {
        val anchoredFeatureTypeConfigs = (allAnchoredFeatures ++ allWindowAggFeatures
          ++ allPassthroughFeatures).flatMap(entry => entry._2.featureAnchor.featureTypeConfigs)
        val derivedFeatureTypeConfigs = allDerivedFeatures.flatMap(entry => entry._2.featureTypeConfigs)
        val seqJoinFeatureTypeConfigs = allSeqJoinFeatures.flatMap(entry => entry._2.featureTypeConfigs)
        anchoredFeatureTypeConfigs ++ derivedFeatureTypeConfigs ++ seqJoinFeatureTypeConfigs
    }
}
