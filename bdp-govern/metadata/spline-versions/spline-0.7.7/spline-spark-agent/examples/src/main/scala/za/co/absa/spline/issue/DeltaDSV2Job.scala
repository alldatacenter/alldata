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

package za.co.absa.spline.issue

import za.co.absa.commons.io.TempDirectory
import za.co.absa.spline.SparkApp

/**
 * This Job requires Spark 3 or higher
 */
object DeltaDSV2Job extends SparkApp(
  name = "DeltaDSV2Job",
  conf = Seq(
    ("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension"),
    ("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog"))
) {
  val path = TempDirectory().deleteOnExit().path

  import za.co.absa.spline.harvester.SparkLineageInitializer._

  // Initializing library to hook up to Apache Spark
  spark.enableLineageTracking()

  spark.sql(s"CREATE DATABASE dsv2 LOCATION '$path'")

  //AppendData
  spark.sql("CREATE TABLE dsv2.ad ( foo String ) USING DELTA")
  spark.sql("INSERT INTO dsv2.ad VALUES ('Mouse')")

  //OverwriteByExpression with condition == true
  spark.sql("CREATE TABLE dsv2.owbe ( foo String ) USING DELTA")
  spark.sql("INSERT OVERWRITE dsv2.owbe  VALUES ('Dog')")

  //OverwriteByExpression with advanced condition
  spark.sql(s"CREATE TABLE dsv2.owbep (ID int, NAME string) USING delta PARTITIONED BY (ID)")
  spark.sql("INSERT OVERWRITE dsv2.owbep PARTITION (ID = 222222) VALUES ('Cat')")

  //CreateTableAsSelect
  spark.sql("CREATE TABLE dsv2.ctas USING DELTA AS SELECT * FROM dsv2.ad;")

  //ReplaceTableAsSelect
  spark.sql(s"CREATE TABLE dsv2.rtas (toBeOrNotToBe boolean) USING delta")
  val data = spark.sql(s"SELECT * FROM dsv2.ad")
  data.write.format("delta")
    .mode("overwrite")
    .option("overwriteSchema", "true")
    .saveAsTable("dsv2.rtas")

}
