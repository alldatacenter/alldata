package com.linkedin.feathr.offline.job

import com.linkedin.feathr.sparkcommon.SeqJoinCustomAggregation
import org.apache.spark.sql.Column
import org.apache.spark.sql.functions.{expr, max}

/*
 * An example class to write own aggregation function for Sequential join. We use the applyAggregation()
 * where we can write any complex aggregation logic. In our case, feathr offline calls the applyAggregation method
 * with map of seqjoined feature to the joined column name. The method should return a map of the seq joined feature
 * name to the aggregated column. Currently, we only support a single joined feature, but in future we will support
 * multiple seq joined features with a single call.
 */
class SeqJoinAggregationClass extends SeqJoinCustomAggregation {

  /**
   * Write your own custom aggregation function, in our case it is just max.
   * Get the seq joined column name for the corresponding seq join feature. Call your custom aggregation function
   * on that column. Return a map of feature name to custom aggregated column.
   * @param featureNameToColumnNameMap  map of seqjoin feature name to corresponding seqjoined column name
   * @return                            map of seq join feature name to custom aggregated column.
   */
  override def applyAggregation(featureNameToColumnNameMap: Map[String, String]): Map[String, Column] = {
    val featureColName = featureNameToColumnNameMap.get("seq_join_a_names_custom")
    Map("seq_join_a_names_custom" -> (max(expr(featureColName.get))))
  }

}
