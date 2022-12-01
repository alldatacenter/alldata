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


package za.co.absa.spline

import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.io.{TempDirectory, TempFile}
import za.co.absa.spline.test.fixture.SparkFixture
import za.co.absa.spline.test.fixture.spline.SplineFixture

class XmlSpec extends AsyncFlatSpec
  with Matchers
  with SparkFixture
  with SplineFixture {

  private val filePath = TempFile("file1", ".xml", false).deleteOnExit().path.toAbsolutePath.toString

  it should "support Xml files as a source" in
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        val testData: DataFrame = {
          val schema = StructType(StructField("ID", IntegerType, nullable = false) :: StructField("NAME", StringType, nullable = false) :: Nil)
          val rdd = spark.sparkContext.parallelize(Row(1014, "Warsaw") :: Row(1002, "Corte") :: Nil)
          spark.sqlContext.createDataFrame(rdd, schema)
        }

        for {
          (plan1, _) <- captor.lineageOf {
            testData
              .write
              .format("xml")
              .option("rootTag", "cities")
              .option("rowTag", "city")
              .mode("overwrite")
              .save(filePath)
          }

          (plan2, _) <- captor.lineageOf {
            val df = spark
              .read
              .format("xml")
              .option("rowTag", "city")
              .load(filePath)

            df.write.save(TempDirectory(pathOnly = true).deleteOnExit().path.toString)
          }
        } yield {
          plan1.operations.write.append shouldBe false
          plan1.operations.write.extra.get("destinationType") shouldBe Some("xml")
          plan1.operations.write.outputSource shouldBe s"file:$filePath"

          plan2.operations.reads.get.head.inputSources.head shouldBe plan1.operations.write.outputSource
          plan2.operations.reads.get.head.extra.get("sourceType") shouldBe Some("xml")
        }
      }
    }

}
