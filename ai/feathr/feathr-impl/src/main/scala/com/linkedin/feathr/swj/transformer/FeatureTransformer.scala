package com.linkedin.feathr.swj.transformer

import com.linkedin.feathr.swj.{FactData, SlidingWindowFeature}
import com.linkedin.feathr.swj.SlidingWindowFeature
import org.apache.spark.sql.{DataFrame, SparkSession}

object FeatureTransformer {

  lazy val spark: SparkSession = SparkSession.builder().getOrCreate()

  private[swj] val GROUP_COL_NAME = "group_col"
  private[swj] val DF_VIEW_NAME = "fact_data"
  private[swj] val JOIN_KEY_COL_NAME = "join_key_swj"
  private[swj] val TIMESTAMP_COL_NAME = "timestamp_col_swj"

  /**
    * Given a sliding window feature specification defined on the fact dataset, which is registered
    * as table fact_data, generate a Tuple of String that will be used to construct the SQL query to
    * transform fact dataset into standardized feature DataFrame.
    *
    * @param feature Feature specification as a [[SlidingWindowFeature]]
    * @return A tuple of String used to construct the SQL query
    */
  private def featureColumnSqlString(feature: SlidingWindowFeature): (String, String) = {
    val hasFilter = feature.filterCondition.isDefined
    val filter = feature.filterCondition.getOrElse("")
    val hasGroupBy = feature.groupBy.isDefined
    val groupCol = feature.groupBy.map(_.groupByCol).getOrElse("")
    val agg = feature.agg
    val partOne =
      s"""
         |struct(${feature.name}_${agg.metricName}
         |${if (hasGroupBy) s", ${feature.name}_$GROUP_COL_NAME" else ""}) AS ${feature.name}
       """.stripMargin
    val partTwo =
      s"""
         |${if (hasFilter)
              s" CASE WHEN $filter THEN ${agg.metricCol} ELSE null END"
            else
              s" ${agg.metricCol}"} AS ${feature.name}_${agg.metricName}
         |${if (hasGroupBy) s", $groupCol AS ${feature.name}_$GROUP_COL_NAME" else ""}
       """.stripMargin
    (partOne, partTwo)
  }

  /**
    * get lateral view sql expression for a feature
    * @param feature
    * @return sql string like: "LATERAL VIEW explode(features) i WHERE i.col.name = 'f1'"
    */
  private def getLateraViewSqlString(feature: SlidingWindowFeature): String = {
    feature.lateralView.map {
      param => {
        s"""
           |LATERAL VIEW ${param.lateralViewDef}
           |${param.lateralViewItemAlias}
           |${param.lateralViewFilter.map("WHERE " + _).getOrElse("")}
    """.stripMargin
      }
    }.getOrElse("")
  }

  /**
    * Given the fact dataset represented as a [[FactData]], convert it into standardized DataFrame
    * which has transformed feature columns plus the join_key and timestamp columns.
    *
    * @param factData A fact dataset with the source DataFrame and all the [[SlidingWindowFeature]]
    *                 defined on this fact dataset
    * @return Transformed feature DataFrame with standardized feature Column
    */
  def transformFactData(factData: FactData): DataFrame = {
    val df = factData.dataSource
    df.createOrReplaceTempView(DF_VIEW_NAME)
    val featureColsSql = factData.aggFeatures.map(featureColumnSqlString)
    require(factData.aggFeatures.map(_.lateralView).distinct.size <= 1,
      "SWA features share same fact data should have same lateral view, or no lateral view")
    val lateralViewSql = getLateraViewSqlString(factData.aggFeatures.head)
    val sql = s"""
                 |SELECT ${featureColsSql.map(_._1).mkString(",")},
                 |$JOIN_KEY_COL_NAME, $TIMESTAMP_COL_NAME FROM
                 |(SELECT
                 |${if (factData.joinKey.size > 1)
                      s"struct(${factData.joinKey.mkString(",")})"
                    else
                      s"${factData.joinKey.head}"
                    } AS $JOIN_KEY_COL_NAME,
                 |${factData.timestampCol} AS $TIMESTAMP_COL_NAME,
                 |${featureColsSql.map(_._2).mkString(",")} FROM
                 |$DF_VIEW_NAME
                 |$lateralViewSql
                 )
       """.stripMargin
    // The order of the feature columns should follow the order of the specified sliding window
    // features. In addition, the feature columns should be in front of join key and timestamp
    // columns. These will guarantee we can fetch corresponding feature columns using index.
    println(s"Generated SQL Statement: ${sql}")
    spark.sql(sql)
  }
}
