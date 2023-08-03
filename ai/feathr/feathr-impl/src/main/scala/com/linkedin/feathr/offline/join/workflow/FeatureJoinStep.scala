package com.linkedin.feathr.offline.join.workflow

import com.linkedin.feathr.common.ErasedEntityTaggedFeature
import com.linkedin.feathr.offline.join.JoinExecutionContext

/**
 * A Feature Join step in Feature Join Workflow.
 * Each join step joins a different types of features (anchored, derived, passthrough and so on).
 * The inputs (and sometimes the outputs) for these steps may look different.
 * We use generics in order to provide this flexibility.
 *
 * @tparam T the type of the input to the step. It should be subclass of [[DataFrameJoinStepInput]].
 * @tparam U the type of the output of the join step. It should be a subclass of [[DataFrameJoinStepOutput]]
 */
private[offline] trait FeatureJoinStep[T <: DataFrameJoinStepInput, U <: DataFrameJoinStepOutput] {

  /**
   * Join specified features to the observation DataFrame.
   * @param features     key tagged features to be joined
   * @param input        input to the step.
   * @param ctx          Join execution context.
   * @return output after joining features.
   */
  def joinFeatures(features: Seq[ErasedEntityTaggedFeature], input: T)(implicit ctx: JoinExecutionContext): U
}
