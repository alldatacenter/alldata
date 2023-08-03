package com.linkedin.feathr.offline.join.algorithms

/**
 * Spark join types, e.g. inner join, left_outer join, etc.
 */
private[offline] object JoinType extends Enumeration {
  type JoinType = Value
  val inner, left_outer, full_outer = Value
}
