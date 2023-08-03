package com.linkedin.feathr

import com.linkedin.feathr.common.DateParam

/**
 * This type alias describes feature data structure for feature join process, which can be shared externally.
 *
 * Sample data : (List(sourceId), nice_a_summary, DateParam(None, None,None, None))
 * Note that "nice_a_summary" is the feature name
 */
package object offline {
  type FeatureName = String
  type KeyTagIdTuple = Seq[Int]
  type JoinKeys = Seq[String]
  type JoinStage = (KeyTagIdTuple, Seq[FeatureName])
  type MaybeFeatureValue = Option[common.FeatureValue]
  type FeatureMapInternal = Map[common.ErasedEntityTaggedFeature, common.FeatureValue]
  type FeatureDataWithJoinKeys = Map[String, (FeatureDataFrame, JoinKeys)]
  type StringTaggedFeatureName = (Seq[String], String, DateParam)
}
