package com.linkedin.feathr.offline.join

import java.time.LocalDateTime

import com.linkedin.feathr.offline.logical.MultiStageJoinPlan
import com.linkedin.feathr.offline.logical.FeatureGroups
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.util.sketch.BloomFilter

/**
 * An Execution Context is the output of logical planning phase.
 * It consists of parameters that determine the execution policy.
 *
 */
private[offline] sealed trait ExecutionContext

/**
 * A wrapper class that wraps run time parameters referenced by most methods.
 * @param sparkSession                 spark session reference
 * @param logicalPlan          analyzed features or logical plan
 * @param featureGroups                feature groupings based on feature defs provided
 * @param bloomFilters                 keyTag to [[BloomFilter]] map. defaults to None.
 * @param frequentItemEstimatedDFMap   join stages to frequentItem estimated DataFrame.
 */
private[offline] case class JoinExecutionContext(
    sparkSession: SparkSession,
    logicalPlan: MultiStageJoinPlan,
    featureGroups: FeatureGroups,
    bloomFilters: Option[Map[Seq[Int], BloomFilter]] = None,
    frequentItemEstimatedDFMap: Option[Map[Seq[Int], DataFrame]] = None)
    extends ExecutionContext
