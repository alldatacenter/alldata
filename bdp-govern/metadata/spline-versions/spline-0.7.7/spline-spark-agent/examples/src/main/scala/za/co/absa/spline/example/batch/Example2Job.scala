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

object Example2Job extends SparkApp("Example 2") {

  import org.apache.spark.sql._
  import za.co.absa.spline.harvester.SparkLineageInitializer._

  spark.enableLineageTracking()

  val ds = spark.read
    .option("header", "true")
    .option("inferSchema", "true")
    .csv("data/input/batch/wikidata.csv")

  // Stage 1
  val startingDS = ds.filter($"total_response_size" > 10000).cache()
  val firstDS = startingDS.filter($"domain_code".eqNullSafe("aa"))
  val secondDS = startingDS.filter($"count_views" > 10)
  val stage1DS = firstDS.union(secondDS)
  stage1DS.write.mode(SaveMode.Overwrite).parquet("data/output/batch/job2_stage1_results")

  // Stage 2
  val stage2DS = spark.read.parquet("data/output/batch/job2_stage1_results")
  stage2DS
    .filter($"domain_code".eqNullSafe("aa"))
    .select($"page_title".as("name"), $"count_views".as("count"))
    .write.mode(SaveMode.Overwrite).parquet("data/output/batch/job2_stage2_results")
}
