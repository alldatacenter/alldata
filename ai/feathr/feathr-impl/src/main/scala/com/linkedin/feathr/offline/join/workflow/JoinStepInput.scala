package com.linkedin.feathr.offline.join.workflow

import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.offline.source.accessor.DataSourceAccessor
import org.apache.spark.sql.DataFrame

/**
 * The trait represents the input of a join step. A join step is where features are joined to the observation data.
 * Hence observation is a required input.
 * @tparam T refers to the type of the underlying data.
 */
private[offline] sealed trait JoinStepInput[T] {
  val observation: T
}

/**
 * The JoinStepInput trait but with DataFrame as the representation of observation data.
 */
private[offline] sealed trait DataFrameJoinStepInput extends JoinStepInput[DataFrame]

/**
 * The base join step input contains observation (and joined features).
 *
 * @param observation observation (with joined features) DataFrame.
 */
private[offline] case class BaseJoinStepInput(observation: DataFrame) extends DataFrameJoinStepInput

/**
 * Input for anchor feature join step. This step takes in a map of anchored feature definition to its source accessor.
 * The source accessor will be used to load the feature data.
 * @param observation             observation DataFrame.
 * @param anchorToSourceAccessor  anchored feature definition to source accessor map.
 */
private[offline] case class AnchorJoinStepInput(observation: DataFrame, anchorToSourceAccessor: Map[FeatureAnchorWithSource, DataSourceAccessor])
    extends DataFrameJoinStepInput
