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
package za.co.absa.spline

import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.SaveMode._
import org.apache.spark.sql.functions._
import org.scalatest.matchers.should.Matchers
import org.scalatest.OneInstancePerTest
import org.scalatest.flatspec.AsyncFlatSpec
import za.co.absa.commons.io.TempDirectory
import za.co.absa.spline.test.fixture.spline.SplineFixture
import za.co.absa.spline.test.fixture.spline.SplineFixture.extractTableIdentifier
import za.co.absa.spline.test.fixture.{SparkDatabaseFixture, SparkFixture}

class InsertIntoHiveTest
  extends AsyncFlatSpec
    with OneInstancePerTest
    with Matchers
    with SparkFixture
    with SparkDatabaseFixture
    with SplineFixture {

  "InsertInto" should "produce lineage when inserting into Hive table" in
    withRestartingSparkContext {
      withCustomSparkSession(_.enableHiveSupport()) { implicit spark =>
        withLineageTracking { captor =>
          val databaseName = s"unitTestDatabase_${this.getClass.getSimpleName}"
          withDatabase(databaseName,
            ("path_archive", "(x String, ymd int) USING hive", Seq(("Tata", 20190401), ("Tere", 20190403))),
            ("path", "(x String) USING hive", Seq("Monika", "Buba"))
          ) {
            val df = spark
              .table("path")
              .withColumn("ymd", lit(20190401))


            for {
              (plan1, _) <- captor.lineageOf {
                df.write.mode(Append).insertInto("path_archive")
              }

              (plan2, _) <- captor.lineageOf {
                spark
                  .read.table("path_archive")
                  .write.csv(TempDirectory(pathOnly = true).deleteOnExit().path.toString)
              }
            } yield {
              plan1.operations.write.append should be(true)
              plan1.operations.write.outputSource should be(s"file:$warehouseDir/${databaseName.toLowerCase}.db/path_archive")
              plan2.operations.reads.get.head.inputSources.head shouldEqual plan1.operations.write.outputSource
            }
          }
        }
      }
    }

  "CsvSerdeTable" should "Produce CatalogTable params" in
    withRestartingSparkContext {
      withCustomSparkSession(_.enableHiveSupport()) { implicit spark =>
        withLineageTracking { captor =>
          withDatabase("test",
            (
              "path_archive_csvserde",
              "(x String, ymd int) ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'",
              Seq(("Tata", 20190401), ("Tere", 20190403))
            ),
            (
              "path_csvserde",
              "(x String) ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'",
              Seq("Monika", "Buba")
            )
          ) {
            val df = spark
              .table("test.path_csvserde")
              .withColumn("ymd", lit(20190401))

            for {
              (plan, _) <- captor.lineageOf {
                df.write.mode(SaveMode.Overwrite).insertInto("test.path_archive_csvserde")
              }
            } yield {
              plan.operations.write.outputSource should include("path_archive")
              plan.operations.write.append should be(false)
              val writeTable = extractTableIdentifier(plan.operations.write.params)
              val readTable = extractTableIdentifier(plan.operations.reads.get.head.params)
              writeTable("table") should be("path_archive_csvserde")
              writeTable("database") should be(Some("test"))
              readTable("table") should be("path_csvserde")
              readTable("database") should be(Some("test"))
            }
          }
        }
      }
    }

  "ParquetSerdeTable" should "Produce CatalogTable params" in
    withRestartingSparkContext {
      withCustomSparkSession(_.enableHiveSupport()) { implicit spark =>
        withLineageTracking { captor =>
          withDatabase("test",
            (
              "path_archive_parquetserde", "(x String, ymd int) stored as PARQUET",
              Seq(("Tata", 20190401), ("Tere", 20190403))
            ),
            (
              "path_parquetserde", "(x String) stored as PARQUET",
              Seq("Monika", "Buba")
            )
          ) {
            val df = spark
              .table("test.path_parquetserde")
              .withColumn("ymd", lit(20190401))

            for {
              (plan, _) <- captor.lineageOf {
                df.write.mode(SaveMode.Overwrite).insertInto("test.path_archive_parquetserde")
              }
            } yield {
              plan.operations.write.outputSource should include("path_archive")
              plan.operations.write.append should be(false)
              val writeTable = extractTableIdentifier(plan.operations.write.params)
              val readTable = extractTableIdentifier(plan.operations.reads.get.head.params)
              writeTable("table") should be("path_archive_parquetserde")
              writeTable("database") should be(Some("test"))
              readTable("table") should be("path_parquetserde")
              readTable("database") should be(Some("test"))
            }
          }
        }
      }
    }

  "OrcSerdeTable" should "Produce CatalogTable params" in
    withRestartingSparkContext {
      withCustomSparkSession(_.enableHiveSupport()) { implicit spark =>
        withLineageTracking { captor =>
          withDatabase("test",
            (
              "path_archive_orcserde", "(x String, ymd int) stored as orc",
              Seq(("Tata", 20190401), ("Tere", 20190403))
            ),
            (
              "path_orcserde", "(x String) stored as orc",
              Seq("Monika", "Buba")
            )
          ) {
            val df = spark
              .table("test.path_orcserde")
              .withColumn("ymd", lit(20190401))

            for {
              (plan, _) <- captor.lineageOf {
                df.write.mode(SaveMode.Overwrite).insertInto("test.path_archive_orcserde")
              }
            } yield {
              plan.operations.write.outputSource should include("path_archive")
              plan.operations.write.append should be(false)

              val writeTable = extractTableIdentifier(plan.operations.write.params)
              val readTable = extractTableIdentifier(plan.operations.reads.get.head.params)
              writeTable("table") should be("path_archive_orcserde")
              writeTable("database") should be(Some("test"))
              readTable("table") should be("path_orcserde")
              readTable("database") should be(Some("test"))
            }
          }
        }
      }
    }
}
