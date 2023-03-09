package com.linkedin.feathr.offline.util

import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.functions.{col, lit, expr}

/**
 * A trait for Splitter Merger.
 * The trait can be used to mock the object DataFrameSplitterMerger, if required.
 */
private[offline] trait DataFrameSplitterMerger {
  def splitOnNull(df: DataFrame, column: String): (DataFrame, DataFrame)
  def split(df: DataFrame, column: String, condition: String): (DataFrame, DataFrame)
  def merge(df: DataFrame, other: DataFrame): DataFrame
}

/**
 * This object encapsulates utility methods to split rows
 * in a DataFrame based on a condition and methods to merge DataFrames.
 */
private[offline] object DataFrameSplitterMerger extends DataFrameSplitterMerger {

  /**
   * Partition input DataFrame into two, one which holds rows with null values for specified column
   * and another with non-null values.
   * @param df      Input DataFrame.
   * @param column  Column to check for nulls.
   * @return        DataFrame tuple - one with non-null values is the first element.
   */
  def splitOnNull(df: DataFrame, column: String): (DataFrame, DataFrame) = {
    (df.filter(df.col(column).isNotNull), df.filter(df.col(column).isNull))
  }

  /**
   * Split a DataFrame based on a column and a condition.
   * For Example: column = z, condition = z > 15.
   * @param df      DataFrame to split.
   * @param column     Column to split the DataFrame on.
   * @param condition  Condition to split the DataFrame.
   * @return
   */
  def split(df: DataFrame, column: String, condition: String): (DataFrame, DataFrame) = {
    val left = df.filter(expr(condition))
    val right = df.join(left, Seq(column), "left_anti")
    (left, right)
  }

  /**
   * Method does a union of input DataFrames.
   * The columns are added or filtered to match the columns in base DataFrame.
   * @param df  Base DataFrame.
   * @param other  DataFrame to coalesce with base DataFrame.
   * @return       coalesced DataFrame.
   */
  def merge(df: DataFrame, other: DataFrame): DataFrame = {
    def buildExpr(dfCols: Seq[String], allCols: Seq[String]): Seq[Column] = {
      val dfColsSet = dfCols.toSet
      allCols.map {
        case x if dfColsSet.contains(x) => col(x)
        case c => lit(null).as(c)
      }
    }
    val allCols = df.columns.toSeq
    val dfCols = other.columns.toSeq
    df.union(other.select(buildExpr(dfCols, allCols): _*))
  }
}
