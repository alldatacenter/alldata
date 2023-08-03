package com.linkedin.feathr.offline.generation.outputProcessor

import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

object FeatureMonitoringUtils {
  def writeToRedis(ss: SparkSession, df: DataFrame, tableName: String, keyColumns: Seq[String], allFeatureCols: Set[String], saveMode: SaveMode): Unit = {
    df.show(10)

    val dfSchema = df.schema
    dfSchema.indices.foreach(index => {
      val field = dfSchema.fields(index)
      val fieldName = field.name
      if (allFeatureCols.contains(fieldName)) {
        field.dataType match {
          case DoubleType | FloatType | IntegerType | LongType =>
            val missing = df.filter(col(fieldName).isNull).count()
            val total = df.count()
//            +------------+------------+----------+----+---+---+---+--------+
//            |feature_name|feature_type|      date|mean|avg|min|max|coverage|
//            +------------+------------+----------+----+---+---+---+--------+
//            |       f_int|     integer|2022-06-09| 0.5|0.5|  0|  1|     1.0|
//            +------------+------------+----------+----+---+---+---+--------+
//
//            +------------+------------+----------+------------------+------------------+-------------------+------------------+--------+
//            |feature_name|feature_type|      date|              mean|               avg|                min|               max|coverage|
//            +------------+------------+----------+------------------+------------------+-------------------+------------------+--------+
//            |    f_double|      double|2022-06-09|0.6061345296768118|0.6061345296768118|0.13751738103840128|0.9651418273038033|     1.0|
//            +------------+------------+----------+------------------+------------------+-------------------+------------------+--------+
            val stats_df = df.select(
              lit(fieldName).name("feature_name"),
              lit(field.dataType.typeName).name("feature_type"),
              current_date().name("date"),
              mean(df(fieldName)).name("mean"),
              avg(df(fieldName)).name("avg"),
              min(df(fieldName)).name("min"),
              max(df(fieldName)).name("max"),
              lit((total - missing) * 1.0 / total).name("coverage")
            )

            stats_df.show()
            writeToSql(ss, stats_df, fieldName, saveMode)
          case StringType | BooleanType =>
            // Will add support for more stats as we have more user requirements
            // The difficulty with term frequency is that it requires a different table other than the scalar stats.
//            val frequencyDf = df
//              .select(
//                lit(fieldName).name("feature_name"),
//                lit(field.dataType.typeName).name("feature_type"),
//                current_date(),
//                col(fieldName),
//              )
//              .groupBy(fieldName)
//              .count()
//              .select(
//                col("*"),
//                lit(fieldName).name("feature_name"),
//                lit(field.dataType.typeName).name("feature_type"),
//                current_date()
//              )
//            writeToSql(frequencyDf, fieldName + "_frequency")

            val missing = df.filter(col(fieldName).isNull).count()
            val total = df.count()
            // cardinality is defined as the number of elements in a set or other grouping, as a property of that grouping.
            val cardinality = df.groupBy(fieldName).count().count()

//            +------------+------------+----------+-----+------+--------+-----------+
//            |feature_name|feature_type|      date|  min|   max|coverage|cardinality|
//            +------------+------------+----------+-----+------+--------+-----------+
//            |    f_string|      string|2022-06-09|apple|orange|     0.9|          3|
//            +------------+------------+----------+-----+------+--------+-----------+
//            +------------+------------+----------+-----+----+--------+-----------+
//            |feature_name|feature_type|      date|  min| max|coverage|cardinality|
//            +------------+------------+----------+-----+----+--------+-----------+
//            |   f_boolean|     boolean|2022-06-09|false|true|     1.0|          2|
//            +------------+------------+----------+-----+----+--------+-----------+
            val stats_df = df.select(
              lit(fieldName).name("feature_name"),
              lit(field.dataType.typeName).name("feature_type"),
              current_date().name("date"),
              min(df(fieldName)).name("min"),
              max(df(fieldName)).name("max"),
              lit((total - missing) * 1.0 / total).name("coverage"),
              lit(cardinality).name("cardinality")
            )

            writeToSql(ss, stats_df, fieldName, saveMode)
          case _ =>
            (rowData: Any) => {
              throw new RuntimeException(f"The data type(${field.dataType}) and data (${rowData}) is not supported in monitoring yet.")
            }
        }
      }
    })
  }

  /**
   * Write the feature monitoring results(usually stats) to SQL database.
   */
  private def writeToSql(ss: SparkSession, stats_df: DataFrame, tableName: String, saveMode: SaveMode): Unit = {
    if (!ss.sparkContext.isLocal) {
      val url = ss.conf.get("monitoring_database_url")
      val username = ss.conf.get("monitoring_database_user")
      val password = ss.conf.get("monitoring_database_password")

      println("monitoring output:")
      println("url: " + url)
      println("username: " + username)

      stats_df.write
        .format("jdbc")
        .option("url", url)
        .option("dbtable", tableName)
        .option("user", username)
        .option("password", password)
        .option("ssl", true)
        .option("sslmode", "require")
        .mode(saveMode)
        .save()
    } else {
      stats_df.show(10)
    }
  }
}
