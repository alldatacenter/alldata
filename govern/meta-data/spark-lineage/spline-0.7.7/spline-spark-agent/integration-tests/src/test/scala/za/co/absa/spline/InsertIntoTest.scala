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
package za.co.absa.spline

import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.functions._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.spline.test.fixture.spline.SplineFixture
import za.co.absa.spline.test.fixture.spline.SplineFixture.extractTableIdentifier
import za.co.absa.spline.test.fixture.{SparkDatabaseFixture, SparkFixture}

class InsertIntoTest extends AsyncFlatSpec
  with Matchers
  with SparkFixture
  with SplineFixture
  with SparkDatabaseFixture {

  "InsertInto" should "not fail when inserting to partitioned table created as Spark tables" in
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        withDatabase("test",
          ("path_archive", "(x String, ymd int) USING json PARTITIONED BY (ymd)",
            Seq(("Tata", 20190401), ("Tere", 20190403))),
          ("path", "(x String) USING json",
            Seq("Monika", "Buba"))
        ) {

          val df = spark
            .table("test.path")
            .withColumn("ymd", lit(20190401))

          for {
            (plan, _) <- captor.lineageOf {
              df.write.mode(SaveMode.Overwrite).insertInto("test.path_archive")
            }
          } yield {
            plan.operations.write.outputSource should include("path_archive")
            plan.operations.write.append should be(false)
          }
        }
      }
    }

  "ParquetTable" should "Produce CatalogTable params" in
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        withDatabase("test",
          ("path_archive", "(x String, ymd int) USING parquet PARTITIONED BY (ymd)",
            Seq(("Tata", 20190401), ("Tere", 20190403))),
          ("path", "(x String) USING parquet",
            Seq("Monika", "Buba"))
        ) {
          val df = spark
            .table("test.path")
            .withColumn("ymd", lit(20190401))

          for {
            (plan, _) <- captor.lineageOf {
              df.write.mode(SaveMode.Overwrite).insertInto("test.path_archive")
            }
          } yield {
            plan.operations.write.outputSource should include("path_archive")
            plan.operations.write.append should be(false)
            val writeTable = extractTableIdentifier(plan.operations.write.params)
            val readTable = extractTableIdentifier(plan.operations.reads.get.head.params)
            writeTable("table") should be("path_archive")
            writeTable("database") should be(Some("test"))
            readTable("table") should be("path")
            readTable("database") should be(Some("test"))
          }
        }
      }
    }

  "CsvTable" should "Produce CatalogTable params" in
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        withDatabase("test",
          ("path_archive", "(x String, ymd int) USING csv PARTITIONED BY (ymd)",
            Seq(("Tata", 20190401), ("Tere", 20190403))),
          ("path", "(x String) USING csv",
            Seq("Monika", "Buba"))
        ) {
          val df = spark
            .table("test.path")
            .withColumn("ymd", lit(20190401))

          for {
            (plan, _) <- captor.lineageOf {
              df.write.mode(SaveMode.Overwrite).insertInto("test.path_archive")
            }
          } yield {
            plan.operations.write.outputSource should include("path_archive")
            plan.operations.write.append should be(false)
            val writeTable = extractTableIdentifier(plan.operations.write.params)
            val readTable = extractTableIdentifier(plan.operations.reads.get.head.params)
            writeTable("table") should be("path_archive")
            writeTable("database") should be(Some("test"))
            readTable("table") should be("path")
            readTable("database") should be(Some("test"))
          }
        }
      }
    }

  "JsonTable" should "Produce CatalogTable params" in
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        withDatabase("test",
          ("path_archive", "(x String, ymd int) USING json PARTITIONED BY (ymd)",
            Seq(("Tata", 20190401), ("Tere", 20190403))),
          ("path", "(x String) USING json",
            Seq("Monika", "Buba"))
        ) {
          val df = spark
            .table("test.path")
            .withColumn("ymd", lit(20190401))

          for {
            (plan, _) <- captor.lineageOf {
              df.write.mode(SaveMode.Overwrite).insertInto("test.path_archive")
            }
          } yield {

            plan.operations.write.outputSource should include("path_archive")
            plan.operations.write.append should be(false)
            val writeTable = extractTableIdentifier(plan.operations.write.params)
            val readTable = extractTableIdentifier(plan.operations.reads.get.head.params)
            writeTable("table") should be("path_archive")
            writeTable("database") should be(Some("test"))
            readTable("table") should be("path")
            readTable("database") should be(Some("test"))
          }
        }
      }
    }

  "ORCTable" should "Produce CatalogTable params" in
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        withDatabase("test",
          ("path_archive", "(x String, ymd int) USING orc PARTITIONED BY (ymd)",
            Seq(("Tata", 20190401), ("Tere", 20190403))),
          ("path", "(x String) USING orc",
            Seq("Monika", "Buba"))
        ) {

          val df = spark
            .table("test.path")
            .withColumn("ymd", lit(20190401))

          for {
            (plan, _) <- captor.lineageOf {
              df.write.mode(SaveMode.Overwrite).insertInto("test.path_archive")
            }
          } yield {
            plan.operations.write.outputSource should include("path_archive")
            plan.operations.write.append should be(false)
            val writeTable = extractTableIdentifier(plan.operations.write.params)
            val readTable = extractTableIdentifier(plan.operations.reads.get.head.params)
            writeTable("table") should be("path_archive")
            writeTable("database") should be(Some("test"))
            readTable("table") should be("path")
            readTable("database") should be(Some("test"))
          }
        }
      }
    }

}
