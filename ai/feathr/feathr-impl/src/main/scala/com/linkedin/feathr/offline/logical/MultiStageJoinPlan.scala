package com.linkedin.feathr.offline.logical

import com.linkedin.feathr.common
import com.linkedin.feathr.offline.{ErasedEntityTaggedFeature, FeatureName, JoinStage, KeyTagIdTuple}

/**
 * A generic trait for a Logical Plan.
 */
private[offline] sealed trait LogicalPlan

/**
 * A concrete materialization of a logical plan.
 * During the planning phase, the requested features are analyzed and an instance of AnalyzedFeatureInfo is output.
 * This class categorizes requested (and required) features to different feature types and also sequences the feature joins to different stages.
 *
 * @param windowAggFeatureStages          join stages to calculate all requested sliding window aggregation features,
 *                                        each join stage contains a set of features that share same key tags, e.g, all
 *                                        x keyed features go to one stage and all y key features go to another stage
 * @param joinStages                      join stages needed to calculate all requested non-sliding window aggregation features
 * @param postJoinDerivedFeatures         all derived features that need to be calculate after all join stages,
 *                                        this is actually some left-over derived features, since we always try
 *                                        to calculate derived features as soon as their dependent features are ready
 * @param requiredWindowAggFeatures       all required sliding window aggregation features
 * @param requiredNonWindowAggFeatures    all required non-sliding window aggregation features
 * @param seqJoinFeatures                 all requested sequential join features
 * @param keyTagIntsToStrings             map key tag id to key tag string
 * @param allRequiredFeatures             all feature required to be computed in order to compute the requested features,
 *                                        i.e., requested features + dependency features
 * @param allRequestedFeatures            all requested featueres in the feature generation/feature join job, only these
 *                                        feature exist in the output dataset of the job
 */
private[offline] case class MultiStageJoinPlan(
    windowAggFeatureStages: Seq[(KeyTagIdTuple, Seq[FeatureName])],
    joinStages: Seq[(KeyTagIdTuple, Seq[FeatureName])],
    postJoinDerivedFeatures: Seq[common.ErasedEntityTaggedFeature],
    requiredWindowAggFeatures: Seq[common.ErasedEntityTaggedFeature],
    requiredNonWindowAggFeatures: Seq[common.ErasedEntityTaggedFeature],
    seqJoinFeatures: Seq[common.ErasedEntityTaggedFeature],
    keyTagIntsToStrings: Seq[String],
    allRequiredFeatures: Seq[common.ErasedEntityTaggedFeature],
    allRequestedFeatures: Seq[common.ErasedEntityTaggedFeature])
    extends LogicalPlan {

  /**
   * Converts features represented as [[com.linkedin.feathr.common.ErasedEntityTaggedFeature]] to [[JoinStage]].
   * @param erasedEntityTaggedFeatures a collection of common.ErasedEntityTaggedFeature.
   * @return collection of join stages.
   */
  def convertErasedEntityTaggedToJoinStage(erasedEntityTaggedFeatures: Seq[common.ErasedEntityTaggedFeature]): Seq[JoinStage] = {
    erasedEntityTaggedFeatures.foldLeft(Seq.empty[JoinStage])((acc, erasedTaggedFeature) => {
      val ErasedEntityTaggedFeature(keyTag, featureName) = erasedTaggedFeature
      acc :+ (keyTag, Seq(featureName))
    })
  }
}
