/*
 * Copyright 2020 ABSA Group Limited
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

package za.co.absa.spline.example.dce

import za.co.absa.spline.SparkApp

object J1StandardizationJob extends SparkApp("My Standardization") {

  import org.apache.spark.sql.functions._
  import za.co.absa.spline.harvester.SparkLineageInitializer._

  spark.enableLineageTracking()

  spark.read
    .csv("data/input/dce/raw/Transactions/2020/01/01/v1/Transactions.csv")
    .withColumn("client_id", '_c0 cast "long")
    .withColumn("date", to_date('_c1, "dd.M.yyyy"))
    .withColumn("merchant", '_c2)
    .withColumn("city", '_c3)
    .withColumn("country", '_c4)
    .withColumn("amount", '_c5 cast "integer")
    .withColumn("currency", '_c6)
    .write
    .mode("overwrite")
    .save("data/_temp/standard/dce/Transactions/2020/01/01/v1")

}
