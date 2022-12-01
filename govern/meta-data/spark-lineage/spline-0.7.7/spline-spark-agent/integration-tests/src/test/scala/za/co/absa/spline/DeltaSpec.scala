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

import org.apache.spark.SPARK_VERSION
import org.apache.spark.sql.SaveMode.Overwrite
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.io.TempDirectory
import za.co.absa.commons.scalatest.ConditionalTestTags._
import za.co.absa.commons.version.Version._
import za.co.absa.spline.test.fixture.SparkFixture
import za.co.absa.spline.test.fixture.spline.SplineFixture

class DeltaSpec extends AsyncFlatSpec
  with Matchers
  with SparkFixture
  with SplineFixture {

  private val deltaPath = TempDirectory(prefix = "delta", pathOnly = true).deleteOnExit().path.toFile.getAbsolutePath

  it should "support Delta Lake as a source" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"2.4.2") in
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        val testData: DataFrame = {
          val schema = StructType(StructField("ID", IntegerType, nullable = false) :: StructField("NAME", StringType, nullable = false) :: Nil)
          val rdd = spark.sparkContext.parallelize(Row(1014, "Warsaw") :: Row(1002, "Corte") :: Nil)
          spark.sqlContext.createDataFrame(rdd, schema)
        }

        for {
          (plan1, Seq(event1)) <- captor.lineageOf {
            testData
              .write
              .format("delta")
              .mode("overwrite")
              .save(deltaPath)
          }

          (plan2, Seq(event2)) <- captor.lineageOf {
            spark
              .read
              .format("delta")
              .load(deltaPath)
              .write
              .mode(Overwrite)
              .saveAsTable("somewhere")
          }

        } yield {
          plan1.id.get shouldEqual event1.planId
          plan2.id.get shouldEqual event2.planId

          plan1.operations.write.append shouldBe false
          plan1.operations.write.extra.get("destinationType") shouldBe Some("delta")
          plan1.operations.write.outputSource shouldBe s"file:$deltaPath"
          plan2.operations.reads.get.head.inputSources.head shouldBe plan1.operations.write.outputSource
          plan2.operations.reads.get.head.extra.get("sourceType") shouldBe Some("parquet")
        }
      }
    }

  it should "support insert into existing Delta Lake table" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"2.4.2") in
    withNewSparkSession { implicit spark =>
      withLineageTracking { lineageCaptor =>
        val testData: DataFrame = {
          val schema = StructType(StructField("ID", IntegerType, nullable = false) :: StructField("NAME", StringType, nullable = false) :: Nil)
          val rdd = spark.sparkContext.parallelize(Row(1014, "Warsaw") :: Row(1002, "Corte") :: Nil)
          spark.sqlContext.createDataFrame(rdd, schema)
        }

        withNewSparkSession {
          _.sql(s"CREATE TABLE table_name(ID int, NAME string) USING DELTA LOCATION '$deltaPath'")
        }

        for {
          (plan1, Seq(event1)) <- lineageCaptor.lineageOf {
            testData.createOrReplaceTempView("tempView")
            spark.sql(s"INSERT OVERWRITE TABLE table_name SELECT * FROM tempView")
          }
        } yield {
          plan1.id.get shouldEqual event1.planId
          plan1.operations.write.append shouldBe false
          plan1.operations.write.extra.get("destinationType") shouldBe Some("delta")
          plan1.operations.write.outputSource shouldBe s"file:$deltaPath"
        }
      }
    }
}
