/*
 * Copyright 2021 ABSA Group Limited
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

package za.co.absa.spline.example.bigquery

import za.co.absa.spline.SparkApp
import za.co.absa.spline.harvester.SparkLineageInitializer

object BigQueryExample extends SparkApp("BigQuery Job") {

  SparkLineageInitializer.enableLineageTracking(spark)

  val InputBQTable = "bigquery-public-data:baseball.games_post_wide"
  val OutputBQTable = s"test.bigquery_result"

  val df1 = spark
    .read
    .format("bigquery")
    .load(InputBQTable)

  val df2 = df1
    .select('gameId, 'hitterFirstName, 'hitterLastName)
    .distinct
    .limit(10)

  df2
    .write
    .format("bigquery")
    .mode("overwrite")
    .save("test.bigquery_result")
}
