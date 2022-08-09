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

object J2ConformanceJob extends SparkApp("My Conformance") {

  import za.co.absa.spline.harvester.SparkLineageInitializer._

  spark.enableLineageTracking()

  val countryMapping = spark.read
    .csv("data/input/dce/mdr/publish/CountryMappingTable/LATEST.csv")
    .withColumnRenamed("_c0", "mdr_code")
    .withColumnRenamed("_c1", "mdr_value")

  val currencyMapping = spark.read
    .csv("data/input/dce/mdr/publish/CurrencyMappingTable/LATEST.csv")
    .withColumnRenamed("_c0", "mdr_code")
    .withColumnRenamed("_c1", "mdr_value")

  val currencyRateMapping = spark.read
    .csv("data/input/dce/mdr/publish/CurrencyRateTable/CZK/LATEST.csv")
    .withColumnRenamed("_c0", "mdr_cur_code")
    .withColumnRenamed("_c1", "mdr_rate")

  spark.read
    .parquet("data/_temp/standard/dce/Transactions/2020/01/01/v1")

    .join(countryMapping, $"country" === $"mdr_value")
    .drop("mdr_value")
    .withColumnRenamed("mdr_code", "ConformedCountry")

    .join(currencyMapping, $"currency" === $"mdr_value")
    .drop("mdr_value")
    .withColumnRenamed("mdr_code", "ConformedCurrency")

    .join(currencyRateMapping, $"ConformedCurrency" === $"mdr_cur_code")
    .withColumn("ConformedAmountCZK", $"amount"*$"mdr_rate")
    .drop("mdr_cur_code")
    .drop("mdr_rate")

    .write
    .mode("overwrite")
    .save("data/output/dce/publish/Transactions/2020/01/01/v1")

}
