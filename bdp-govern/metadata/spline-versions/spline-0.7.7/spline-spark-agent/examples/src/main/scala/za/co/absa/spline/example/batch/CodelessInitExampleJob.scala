/*
 * Copyright 2019 ABSA Group Limited
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

import org.apache.spark.sql.SaveMode
import za.co.absa.spline.SparkApp

/**
  * An example job where no explicit Spline code was used to initialize Spline. Only Spark configuration was used.
  */
object CodelessInitExampleJob extends SparkApp(
  name ="Codeless Init Example Job",
  // Spark configuration used to register Spline listener for codeless init.
  conf = Seq(("spark.sql.queryExecutionListeners", "za.co.absa.spline.harvester.listener.SplineQueryExecutionListener"))) {

  // A business logic of a spark job ...
  spark.read
    .option("header", "true")
    .option("inferSchema", "true")
    .csv("data/input/batch/wikidata.csv")
    .as("source")
    .filter($"total_response_size" > 1000)
    .filter($"count_views" > 10)
    .select($"page_title".as("page"), $"count_views")
    .write.mode(SaveMode.Overwrite).parquet("data/output/batch/codeless_init_job_results")
}
