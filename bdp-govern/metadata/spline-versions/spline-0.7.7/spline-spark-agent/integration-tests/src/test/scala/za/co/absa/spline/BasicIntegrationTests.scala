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

import org.apache.spark.internal.Logging
import org.apache.spark.sql.Row
import org.apache.spark.sql.SaveMode._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{IntegerType, StructField, StructType}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.io.TempDirectory
import za.co.absa.spline.test.fixture.SparkFixture
import za.co.absa.spline.test.fixture.spline.SplineFixture

class BasicIntegrationTests extends AsyncFlatSpec
  with Matchers
  with SparkFixture
  with SplineFixture
  with Logging {

  "saveAsTable" should "process all operations" in
    withNewSparkSession(implicit spark =>
      withLineageTracking { captor =>
        import spark.implicits._

        withNewSparkSession {
          _.sql("drop table if exists someTable")
        }

        for {
          (plan, _) <- captor.lineageOf {
            val df = Seq((1, 2), (3, 4)).toDF().agg(concat(sum('_1), min('_2)) as "forty_two")
            df.write.saveAsTable("someTable")
          }
        } yield {
          plan.operations.reads should be(None)
          plan.operations.other.get should have length 2
          plan.operations.write should not be null
        }
      }
    )

  "save_to_fs" should "process all operations" in
    withNewSparkSession(implicit spark =>
      withLineageTracking { captor =>
        import spark.implicits._
        val path = TempDirectory("spline_", ".parquet", pathOnly = true).deleteOnExit().path.toString

        for {
          (plan, _) <- captor.lineageOf {
            val df = Seq((1, 2), (3, 4)).toDF().agg(concat(sum('_1), min('_2)) as "forty_two")
            df.write.save(path)
          }
        } yield {
          plan.operations.reads should be(None)
          plan.operations.other.get should have length 2
          plan.operations.write should not be null
        }
      }
    )

  "saveAsTable" should "produce URIs compatible with filesystem write" in
    withNewSparkSession(implicit spark =>
      withLineageTracking { captor =>
        val tableName = "externalTable"
        val path = TempDirectory("spline", ".table").deleteOnExit().path.toUri

        withNewSparkSession {
          _.sql(s"create table $tableName (num int) using parquet location '$path' ")
        }

        val schema: StructType = StructType(List(StructField("num", IntegerType, nullable = true)))
        val data = spark.sparkContext.parallelize(Seq(Row(1), Row(3)))
        val inputDf = spark.sqlContext.createDataFrame(data, schema)

        for {
          (plan1, _) <- captor.lineageOf(inputDf.write.mode(Append).saveAsTable(tableName))
          (plan2, _) <- captor.lineageOf(inputDf.write.mode(Overwrite).save(path.toString))
        } yield {
          plan1.operations.write.outputSource should be(plan2.operations.write.outputSource)
        }
      })

  "saveAsTable and read.table" should "produce equal URIs" in
    withNewSparkSession(implicit spark =>
      withLineageTracking { captor =>
        val tableName = "externalTable"
        val path = TempDirectory("spline", ".table").deleteOnExit().path.toUri

        withNewSparkSession { innerSpark =>
          innerSpark.sql(s"drop table if exists $tableName")
          innerSpark.sql(s"create table $tableName (num int) using parquet location '$path' ")
        }

        val schema: StructType = StructType(List(StructField("num", IntegerType, nullable = true)))
        val data = spark.sparkContext.parallelize(Seq(Row(1), Row(3)))

        for {
          (plan1, _) <- captor.lineageOf {
            spark
              .sqlContext.createDataFrame(data, schema)
              .write.mode(Append).saveAsTable(tableName)
          }

          (plan2, _) <- captor.lineageOf {
            spark
              .read.table(tableName)
              .write.mode(Overwrite).saveAsTable("somewhere")
          }
        } yield {
          println("yield")
          val writeUri = plan1.operations.write.outputSource
          val readUri = plan2.operations.reads.get.head.inputSources.head

          writeUri shouldEqual readUri
        }
      })

  "saveAsTable" should "produce table path as identifier when writing to external table" in
    withNewSparkSession(implicit spark =>
      withLineageTracking { captor =>
        val path = TempDirectory("spline", ".table", pathOnly = true).deleteOnExit().path

        withNewSparkSession {
          _.sql(s"create table e_table(num int) using parquet location '${path.toUri}'")
        }

        for {
          (plan, _) <- captor.lineageOf {
            val schema: StructType = StructType(List(StructField("num", IntegerType, nullable = true)))
            val data = spark.sparkContext.parallelize(Seq(Row(1), Row(3)))
            val df = spark.sqlContext.createDataFrame(data, schema)

            df.write.mode(Append).saveAsTable("e_table")
          }
        } yield {
          plan.operations.write.outputSource should be(path.toFile.toURI.toString.init)
        }
      })

}
