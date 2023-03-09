package com.linkedin.feathr.offline.join

import com.linkedin.feathr.offline.client.DataFrameColName
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{col, concat_ws, expr, when}

/**
 * This class is used to combine multiple key columns in a dataframe.
 * It's used for bloom filter and salted join, could only be set up on one column of primitive types.
 * As a result, we need to concatenate multiple columns into one before setting up bloom filter.
 */
private[offline] class DataFrameKeyCombiner {

  /**
   * For a DataFrame, concatenate multiple columns to generate a new column.
   * The name of the new column is the list of columns concatenated with "#",
   * and the value of the new column is the value of each column concatenated with "#".
   *
   *
   * @param df the input DataFrame
   * @param columnNameExprs A list of column names or expressions. The concatenation happens by the order of this list.
   * @param filterNull whether to filter out the null keys in the result dataframe.
   * @return (new concatenated column's name, DataFrame with the new column), if any of the key is null, the row will be removed
   */
  def combine(df: DataFrame, columnNameExprs: Seq[String], filterNull: Boolean = true): (String, DataFrame) = {
    val newColName = DataFrameColName.generateJoinKeyColumnName(columnNameExprs)
    if (columnNameExprs.size > 1) {
      // use special value to replace null in the column so that we can figure out which row has at least one null in the keys,
      // we use a special string value, otherwise the null value will be string 'null' in the output, which is not special
      // enough to determine existence of null value
      val nullElementGuardString = if (filterNull) "__feathr_internal_null_join_key_" else "__feathr_null"
      val newColExpr = concat_ws("#", columnNameExprs.map(c => {
        val casted = expr(s"CAST (${c} as string)")
        // If any key in the keys is null, replace with special value and remove the row later
        when(casted.isNull, nullElementGuardString).otherwise(casted)
      }): _*)
      val withKeyDF = df.withColumn(newColName, newColExpr)
      // filter out rows with at least one null value(contains the special value we replaced earlier) in the key
      val resultDF = if (filterNull) withKeyDF.filter(!col(newColName).contains(nullElementGuardString)) else withKeyDF
      (newColName, resultDF)
    } else {
      val withKeyDF = df.withColumn(newColName, expr(s"CAST (${columnNameExprs.head} AS string)"))
      val resultDF = if (filterNull) withKeyDF.filter(col(newColName).isNotNull) else withKeyDF
      (newColName, resultDF)
    }
  }
}

object DataFrameKeyCombiner {
  private val singleton = new DataFrameKeyCombiner();
  def apply(): DataFrameKeyCombiner = singleton
}
