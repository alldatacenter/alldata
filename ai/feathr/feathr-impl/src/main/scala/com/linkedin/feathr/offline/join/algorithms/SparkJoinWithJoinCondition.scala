package com.linkedin.feathr.offline.join.algorithms


import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrFeatureJoinException}
import com.linkedin.feathr.offline.join.algorithms.JoinType.{JoinType, _}
import org.apache.spark.sql.DataFrame

/**
 * Implementation of basic spark joiner. This implementation takes in the join condition builder as input.
 */
private[offline] class SparkJoinWithJoinCondition private (joinConditionBuilder: SparkJoinConditionBuilder) extends SparkJoin {

  /**
   * This API joins the "left" / "build" and "right" / "probe" sides.
   * The API abstracts the underlying join algorithm or join condition.
   *
   * @param leftJoinColumns  join columns of the left DataFrame.
   * @param leftDF           left DataFrame or the build side of the join.
   * @param rightJoinColumns join columns of the right DataFrame.
   * @param rightDF          right DataFrame or the probe side of the join.
   * @param joinType         the join type for the join, say "left_outer", "inner" etc
   * @return joined DataFrame
   */
  override def join(leftJoinColumns: Seq[String], leftDF: DataFrame, rightJoinColumns: Seq[String], rightDF: DataFrame, joinType: JoinType): DataFrame = {
    if (leftJoinColumns.size != rightJoinColumns.size) {
      throw new FeathrFeatureJoinException(
        ErrorLabel.FEATHR_ERROR,
        s"left join key column size is not equal to right join key column size. " +
          s"Left join columns: [${leftJoinColumns.mkString(", ")}], Right join columns: [${rightJoinColumns.mkString(", ")}]")
    } else if (!joinType.equals(left_outer) && !joinType.equals(inner)) {
      throw new FeathrFeatureJoinException(ErrorLabel.FEATHR_ERROR, s"Feathr currently does not support join type ${joinType.toString}")
    } else {
      //  current behavior is keeping one record for duplicate records(same as the original RDD API)
      val withoutDupRightDF = rightDF.dropDuplicates(rightJoinColumns)
      val joinConditions = joinConditionBuilder.buildJoinCondition(leftJoinColumns, leftDF, rightJoinColumns, rightDF)
      leftDF.join(withoutDupRightDF, joinConditions, joinType.toString)
    }
  }
}

/**
 * The class instantiation is delegated to companion object.
 */
private[offline] object SparkJoinWithJoinCondition {
  def apply(joinConditionBuilder: SparkJoinConditionBuilder): SparkJoinWithJoinCondition = new SparkJoinWithJoinCondition(joinConditionBuilder)
}
