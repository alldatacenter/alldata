package com.linkedin.feathr.offline.join.workflow

import com.linkedin.feathr.offline.FeatureDataFrame

/**
 * The trait represents the output of a join step. The output should always contain observation joined with features.
 *
 * @tparam T refers to the underlying representation of step output data.
 */
private[offline] sealed trait JoinStepOutput[T] {
  val obsAndFeatures: T
}

/**
 * JoinStepOutput with FeatureDataFrame as the representation of join output.
 */
private[offline] sealed trait DataFrameJoinStepOutput extends JoinStepOutput[FeatureDataFrame]

/**
 * This class is the basic output of a join step. This includes [[FeatureDataFrame]],
 * DataFrame (feature joined observation data) and inferred feature types for the joined features.
 */
private[offline] case class FeatureDataFrameOutput(obsAndFeatures: FeatureDataFrame) extends DataFrameJoinStepOutput
