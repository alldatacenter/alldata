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

object ExcelJob extends SparkApp("Excel Job", conf = Seq("spark.sql.shuffle.partitions" -> "4")) {

  // Initializing library to hook up to Apache Spark
  import za.co.absa.spline.harvester.SparkLineageInitializer._

  spark.enableLineageTracking()

  val df = spark.read
    .format("com.crealytics.spark.excel")
    .option("header", "true")
    .load("data/input/batch/test.xlsx")

  val res = df.select($"number" + 1, $"text")

  res.write
    .format("com.crealytics.spark.excel")
    .option("dataAddress", "'My Sheet'!B3:C35")
    .option("header", "true")
    .mode("overwrite")
    .save("data/output/batch/result.xlsx")
}
