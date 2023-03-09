package com.linkedin.feathr.offline.util

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._

object Transformations {

  /**
   * Sorts the column names of a DataFrame
   */
  def sortColumns(order: String = "asc")(df: DataFrame): DataFrame = {
    val colNames = if (order == "asc") {
      df.columns.sorted
    } else if (order == "desc") {
      df.columns.sorted.reverse
    } else {
      throw new IllegalArgumentException(s"Order for sortColumns must be 'asc' or 'desc', but got ${order}")
    }
    val cols = colNames.map(col(_))
    df.select(cols: _*)
  }
}
