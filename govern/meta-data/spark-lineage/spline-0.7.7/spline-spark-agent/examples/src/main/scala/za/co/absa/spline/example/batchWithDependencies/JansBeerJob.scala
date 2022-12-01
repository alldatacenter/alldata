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

package za.co.absa.spline.example.batchWithDependencies

import za.co.absa.spline.SparkApp

object JansBeerJob extends SparkApp("Jan's Beer Job", conf = Seq("spark.sql.shuffle.partitions" -> "4")) {

  import org.apache.spark.sql._
  import org.apache.spark.sql.functions._

  // Initializing library to hook up to Apache Spark
  import za.co.absa.spline.harvester.SparkLineageInitializer._

  spark.enableLineageTracking()

  spark.sparkContext.setLogLevel("TRACE")

  val beerConsumption = spark.read.option("header", "true").csv("data/input/batchWithDependencies/beerConsum.csv")
  val population = spark.read.option("header", "true").csv("data/input/batchWithDependencies/population.csv")

  def calculateConsumptionPerCapita(year: String) =
    (col(year) * 100) / col(s"y$year") as s"Year$year"


  val result = beerConsumption
    .join(population, $"Code" === $"Country Code", "inner")
    .select(
      $"Country",
      $"Code",
      calculateConsumptionPerCapita("2003"),
      calculateConsumptionPerCapita("2004"),
      calculateConsumptionPerCapita("2005"),
      calculateConsumptionPerCapita("2006"),
      calculateConsumptionPerCapita("2007"),
      calculateConsumptionPerCapita("2008"),
      calculateConsumptionPerCapita("2009"),
      calculateConsumptionPerCapita("2010"),
      calculateConsumptionPerCapita("2011")
    )

  result.write.mode(SaveMode.Append).parquet("data/output/batchWithDependencies/beerConsCtl")

}
