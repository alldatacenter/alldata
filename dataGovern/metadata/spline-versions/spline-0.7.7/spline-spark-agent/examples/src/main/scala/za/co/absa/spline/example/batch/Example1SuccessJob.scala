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

object Example1SuccessJob extends SparkApp("Example 1 (successful)") {

  import org.apache.spark.sql._
  import za.co.absa.spline.harvester.SparkLineageInitializer._

  // Initializing library to hook up to Apache Spark
  spark.enableLineageTracking()

  // A business logic of a spark job ...

  val sourceDS = spark.read
    .option("header", "true")
    .option("inferSchema", "true")
    .csv("data/input/batch/wikidata.csv")
    .as("source")
    .filter($"total_response_size" > 1000)
    .filter($"count_views" > 10)

  val domainMappingDS = spark.read
    .option("header", "true")
    .option("inferSchema", "true")
    .csv("data/input/batch/domain.csv")
    .as("mapping")

  val joinedDS = sourceDS
    .join(domainMappingDS, $"domain_code" === $"d_code", "left_outer")
    .select($"page_title".as("page"), $"d_name".as("domain"), $"count_views")

  joinedDS.write.mode(SaveMode.Append).parquet("data/output/batch/job1_results")
}
