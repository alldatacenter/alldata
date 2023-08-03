package com.linkedin.feathr.offline.join.algorithms


import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrFeatureJoinException}
import com.linkedin.feathr.offline.join.algorithms.JoinType.JoinType
import org.apache.spark.sql.DataFrame

/**
 * Implementation of spark equi join where no join condition is used to join the 2 DataFrames.
 * The probe (right) side columns are renamed to match the build (left) side columns and then joined.
 */
private[offline] class SparkJoinWithNoJoinCondition extends SparkJoin {
  override def join(leftJoinColumns: Seq[String], leftDF: DataFrame, rightJoinColumns: Seq[String], rightDF: DataFrame, joinType: JoinType): DataFrame = {
    if (leftJoinColumns.size != rightJoinColumns.size) {
      throw new FeathrFeatureJoinException(
        ErrorLabel.FEATHR_ERROR,
        s"left join key column size is not equal to right join key column size. " +
          s"Left join columns: [${leftJoinColumns.mkString(", ")}], Right join columns: [${rightJoinColumns.mkString(", ")}]")
    }

    // Rename the columns
    val renamedRightDF = rightJoinColumns
      .zip(leftJoinColumns)
      .foldLeft(rightDF)((baseDF, namePair) => {
        baseDF.withColumnRenamed(namePair._1, namePair._2)
      })

    leftDF.join(renamedRightDF, leftJoinColumns, joinType.toString)
  }
}

/**
 * Companion object.
 */
private[offline] object SparkJoinWithNoJoinCondition {
  def apply(): SparkJoinWithNoJoinCondition = new SparkJoinWithNoJoinCondition()
}
