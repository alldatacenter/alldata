package com.linkedin.feathr.offline.transformation

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.{lit, monotonically_increasing_id, row_number}
import org.apache.spark.sql.types.StructField

private[feathr] object DataFrameExt {
  implicit class DataFrameMethods(df: DataFrame) {

    /**
     * Extend the input dataframe with column provided in the schema, if it do not exist in the input dataframe
     * @param inputDF extend the input Dataframe with all fields in the input schema if it does not exist in input DF
     * @param schema schema with fields to be extended
     * @return extended input dataframe
     */
    private def extendDf(inputDF: DataFrame, schema: Seq[StructField]): DataFrame = {
      // Find fields from the schema which do not exist in the input dataframe
      val extendFieldNames = schema.map(_.name).distinct.filter(f => !inputDF.schema.map(_.name).contains(f))
      val schemaMap = schema.map(field => field.name -> field).toMap
      // Extend the fields by filling null values
      extendFieldNames.foldLeft(inputDF)((curDf, field) => {
        curDf.withColumn(field, lit(null) cast schemaMap(field).dataType)
      })
    }

    /**
     * The difference between this function and [[union]] is that this function
     * resolves columns by name (not by position).
     * The set of column names in this and other Dataset can differ.
     * missing columns will be filled with null.
     *
     * @param other other dataframe for the union
     * @return unioned dataframe
     */
    def fuzzyUnion(other: DataFrame): DataFrame = {
      val schema = df.schema.union(other.schema)
      val extendedDf: DataFrame = extendDf(df, schema)
      val extendedOtherDf = extendDf(other, schema)
      extendedDf.unionByName(extendedOtherDf)
    }


    /**
     * Copy the value of rightJoinColumns from rightDF to the current DataFrame, and rename as leftJoinColumns columns
     * e.g.
     * left          right
     * id1 id2 v1 v2       id3 id4 v3
     * 0    5  a   b        0   5   e
     * 1    6  c   d        10  11  f
     *
     * will return:
     * id1 id2 v1 v2
     * 0  5  a  b
     * 1  6  c  d
     * 0  5  a  b
     * 10  11  c d
     *
     * @param leftJoinColumns
     * @param rightJoinColumns
     * @param rightDF
     * @return
     */
    def appendRows(leftJoinColumns: Seq[String], rightJoinColumns: Seq[String], rightDF: DataFrame): DataFrame = {
      // Add a few more rows to observation data by copying the feature key from the feature dataset
      // This ensures the join will have matched results between observation and feature data
      val dfCount = df.count()
      val rightCount = rightDF.count()
      val leftDf = if (dfCount < rightCount && dfCount > 0 && rightCount > 0) {
        val ratio = rightCount.toDouble / dfCount
        df.sample(true, ratio, 2)
      } else {
        df
      }
      val indexIdColExpr = row_number.over(Window.orderBy(monotonically_increasing_id)) - 1
      val indexColName = "sanity_check_run_join_index_col"
      val withIndexLeftDF = leftDf.withColumn(indexColName, indexIdColExpr)
      val withIndexRightJoinKeysDF = rightDF.select(rightJoinColumns.head, rightJoinColumns.tail: _*)
        .withColumn(indexColName, indexIdColExpr)
      // Rename rightDF column to avoid clash with current DF after join
      val newRightJoinColumns = rightJoinColumns.map(col => "_temp_right_" + col)
      val renamedWithIndexRightJoinKeysDF = rightJoinColumns.zip(newRightJoinColumns).foldLeft(withIndexRightJoinKeysDF)((baseDF, joinKeyColPair) =>
        baseDF.withColumnRenamed(joinKeyColPair._1, joinKeyColPair._2)
        )
      val leftWithRightJoinKeyDF = withIndexLeftDF.join(renamedWithIndexRightJoinKeysDF, indexColName)
        .drop(leftJoinColumns : _*).drop(indexColName)
      val renamedLeftDf = leftJoinColumns.zip(newRightJoinColumns).foldLeft(leftWithRightJoinKeyDF)((baseDF, joinKeyColPair) =>
        baseDF.withColumnRenamed(joinKeyColPair._2, joinKeyColPair._1))
      leftDf.unionByName(renamedLeftDf)
    }
  }
}
