package com.linkedin.feathr.offline.join.algorithms


import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrFeatureJoinException}
import org.apache.spark.sql.functions.expr
import org.apache.spark.sql.types.{ArrayType, NumericType, StringType, StructType}
import org.apache.spark.sql.{Column, DataFrame}

/**
 * Different join algorithms support different join conditions. Some joins may support only equality joins,
 * others range conditions and so on. This interface is provides template for implementing the join condition builder.
 * The joins are composed with the condition builders.
 * @tparam T the type of the left and right side of the join.
 * @tparam R the return type of the condition builder or the type of the constructed condition.
 */
private[offline] sealed trait JoinConditionBuilder[T, R] extends Serializable {

  /**
   * Builds the join condition for the inputs of the join. The join condition is dependent on the type of leftJoinColumn.
   * Note: This is just a condition builder. The validation of left and right join keys should be done in the Join algorithm.
   * @param leftJoinColumns   join column names of the left side of join (build side).
   * @param left              left or build side data.
   * @param rightJoinColumns  join column names of the right side of join (probe side).
   * @param right             right or probe side data.
   * @return
   */
  def buildJoinCondition(leftJoinColumns: Seq[String], left: T, rightJoinColumns: Seq[String], right: T): R
}

/**
 * Spark condition builder trait takes in DataFrame as input type.
 * The output "Column" type aligns with the Spark SQL interfaces where join conditions are considered
 * as column transformations and return a "Column".
 */
private[offline] sealed trait SparkJoinConditionBuilder extends JoinConditionBuilder[DataFrame, Column]

/**
 * Builds the equality join condition for input DataFrames.
 */
private[offline] object EqualityJoinConditionBuilder extends SparkJoinConditionBuilder {
  override def buildJoinCondition(leftJoinColumns: Seq[String], left: DataFrame, rightJoinColumns: Seq[String], right: DataFrame): Column = {
    leftJoinColumns
      .zip(rightJoinColumns)
      .map { case (leftKey, rightKey) => left(leftKey) === right(rightKey) }
      .reduce(_ and _)
  }
}

/**
 * Builds the join condition for input DataFrames. The join condition is dependent on the type of leftJoinColumn.
 * Type becomes important here because the disparate feature value types participate in the join as keys and
 * the join is followed by an aggregation.
 */
private[offline] object SequentialJoinConditionBuilder extends SparkJoinConditionBuilder {
  override def buildJoinCondition(leftJoinColumns: Seq[String], left: DataFrame, rightJoinColumns: Seq[String], right: DataFrame): Column = {
    leftJoinColumns
      .zip(rightJoinColumns)
      .map {
        case (x, y) =>
          val leftFieldIndex = left.schema.fieldIndex(x.split("\\.").head) // regex to support complex keys.
          val leftFieldType = left.schema.toList(leftFieldIndex)
          leftFieldType.dataType match {
            case _: StringType => left(x) === right(y)
            case _: NumericType => left(x) === right(y)
            case _: StructType => left(x) === right(y)
            case _: ArrayType => expr(s"array_contains(${x}, ${y})")
            case _ =>
              throw new FeathrFeatureJoinException(
                ErrorLabel.FEATHR_ERROR,
                s"The left feature key ${x} is of ${leftFieldType.dataType} type. Currently in Sequential Join, we only support " +
                  s"StringType, NumericType and ArrayType columns as left join columns.")
          }
      }
      .reduce(_ && _)
  }
}
