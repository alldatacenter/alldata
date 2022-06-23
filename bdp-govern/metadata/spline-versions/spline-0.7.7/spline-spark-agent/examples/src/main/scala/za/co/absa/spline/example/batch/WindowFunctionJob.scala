/*
 * Copyright 2017 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.example.batch

import za.co.absa.spline.SparkApp

/**
 * Example taken from:
 * https://databricks.com/blog/2015/07/15/introducing-window-functions-in-spark-sql.html
 *
 */
object WindowFunctionJob extends SparkApp("Window Function Job") {

  import org.apache.spark.sql._
  import org.apache.spark.sql.expressions._
  import org.apache.spark.sql.functions._
  import org.apache.spark.sql.types._
  import za.co.absa.spline.harvester.SparkLineageInitializer._

  // Initializing library to hook up to Apache Spark
  spark.enableLineageTracking()

  // A business logic of a spark job ...
  val testData: DataFrame = {
    val schema = StructType(List(
      StructField("product", StringType, nullable = false),
      StructField("category", StringType, nullable = false),
      StructField("revenue", IntegerType, nullable = false)
    ))
    val rdd = spark.sparkContext.parallelize(Seq(
      Row("Thin", "Cell phone", 6000),
      Row("Normal", "Tablet", 1500),
      Row("Mini", "Tablet", 5500),
      Row("Ultra thin", "Cell phone", 5000),
      Row("Very thin", "Cell phone", 6000),
      Row("Big", "Tablet", 2500),
      Row("Bendable", "Cell phone", 3000),
      Row("Foldable", "Cell phone", 3000),
      Row("Pro", "Tablet", 4500),
      Row("Pro2", "Tablet", 6500)
    ))
    spark.sqlContext.createDataFrame(rdd, schema)
  }

  val windowSpec = Window
    .partitionBy("category")
    .orderBy("revenue")
    .rangeBetween(Window.unboundedPreceding, Window.unboundedFollowing)

  val dataFrame = testData
  val revenue_difference = max("revenue").over(windowSpec) - dataFrame("revenue")

  dataFrame
    .select(
      dataFrame("product"),
      dataFrame("category"),
      dataFrame("revenue"),
      revenue_difference.alias("revenue_difference"))
    .write
    .format("com.crealytics.spark.excel")
    .option("header", "true")
    .mode("overwrite")
    .save("data/output/batch/window_function_job_result")
}
