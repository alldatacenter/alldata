package com.linkedin.feathr.offline.join.algorithms

import com.linkedin.feathr.offline.join.algorithms.JoinType.JoinType
import org.apache.spark.sql.DataFrame

/**
 * We have different join algorithm/optimization strategy, e.g.
 * spark native join, salted join, slickJoin, adaptive join, etc.
 * They can all implement this interface.
 * This trait serves as the interface for implementing the join algorithm in Feathr Offline.
 * It makes the following assumptions:
 * - A join should have a build (left) side and probe (right) side.
 * - A join is defined on a collection of columns.
 * - A join has a type based on how the build and probe sides are joined (left_outer, inner etc).
 */
private[offline] trait Join[T] extends Serializable {

  /**
   * This API joins the "left" / "build" and "right" / "probe" sides.
   * The API abstracts the underlying join algorithm or join condition.
   * @param leftJoinColumns   join columns of the left side of join (build side).
   * @param left              left or build side data.
   * @param rightJoinColumns  join columns of the right side of join (probe side).
   * @param right             right or probe side data.
   * @param joinType          the join type for the join, say "left_outer", "inner" etc
   * @return joined output
   */
  def join(leftJoinColumns: Seq[String], left: T, rightJoinColumns: Seq[String], right: T, joinType: JoinType): T
}

/**
 * This interface ties underlying representation of data to Spark DataFrame.
 */
private[offline] trait SparkJoin extends Join[DataFrame]
