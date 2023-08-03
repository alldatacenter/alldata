package com.linkedin.feathr.sparkcommon

import org.apache.spark.sql.Column

/*
 * Base class to implement custom aggregation function on top of sequential join.
 * Feathr calls applyAggregation by passing in a map of the feature name to the sequentially joined
 * column name. applyAggregation method can create any custom aggregation for each of the seqJoin
 * features, and return back a map from the feature name to the column with the custom aggregation applied.
 * Then feathr will replace original unaggregated column with this column.
 */

abstract class SeqJoinCustomAggregation extends Serializable {
  /**
    * Apply a custom aggregation function on seqJoin features.
    * @param featureNameToColumnNameMap  Map from seqjoin feature names to the joined, unaggregated column name. This
    *                                    column name will generally look like
    *                                    "__feathr_feature_<feature-name>__feathr_tags_<randomString>".
    * @return Map from seqJoin feature name to column with custom aggregation.
    */
  def applyAggregation(featureNameToColumnNameMap : Map[String, String]): Map[String, Column]
}
