package com.linkedin.feathr.offline.logical

import com.linkedin.feathr.common.JoiningFeatureParams

/**
 * This class takes the responsibility of generating a logical plan for joining / generating features.
 */
private[offline] trait LogicalPlanner[T <: LogicalPlan] {

  /**
   * The method outputs a [[LogicalPlan]] for the requested input features.
   * This plan will contain information regarding all required features and a plan for joining the features.
   * @param featureGroups [[FeatureGroups]] object.
   * @param keyTaggedFeatures requested features and its key tag
   * @return logical plan
   */
  def getLogicalPlan(featureGroups: FeatureGroups, keyTaggedFeatures: Seq[JoiningFeatureParams]): T
}
