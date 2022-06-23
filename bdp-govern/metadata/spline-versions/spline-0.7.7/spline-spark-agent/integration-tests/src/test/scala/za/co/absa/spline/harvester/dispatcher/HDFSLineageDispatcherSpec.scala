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

package za.co.absa.spline.harvester.dispatcher

import org.apache.commons.io.FileUtils.readFileToString
import org.apache.spark.SPARK_VERSION
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.io.TempDirectory
import za.co.absa.commons.json.DefaultJacksonJsonSerDe
import za.co.absa.commons.scalatest.ConditionalTestTags.ignoreIf
import za.co.absa.commons.version.Version._
import za.co.absa.spline.test.fixture.SparkFixture
import za.co.absa.spline.test.fixture.spline.SplineFixture

import java.io.File

class HDFSLineageDispatcherSpec
  extends AsyncFlatSpec
    with Matchers
    with SparkFixture
    with SplineFixture
    with DefaultJacksonJsonSerDe {

  behavior of "HDFSLineageDispatcher"

  it should "save lineage file to a filesystem" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"2.3") in {
    withCustomSparkSession(_
      .config("spark.spline.lineageDispatcher", "hdfs")
      .config("spark.spline.lineageDispatcher.hdfs.className", classOf[HDFSLineageDispatcher].getName)
    ) { implicit spark =>
      withLineageTracking { captor =>
        import spark.implicits._
        val dummyDF = Seq((1, 2)).toDF
        val destPath = TempDirectory("spline_", ".parquet", pathOnly = true).deleteOnExit().path

        for {
          (_, _) <- captor.lineageOf(dummyDF.write.save(destPath.toString))
        } yield {
          val lineageFile = new File(destPath.toFile, "_LINEAGE")
          lineageFile.exists should be(true)
          lineageFile.length should be > 0L

          val lineageJson = readFileToString(lineageFile, "UTF-8").fromJson[Map[String, Map[String, _]]]
          lineageJson should contain key "executionPlan"
          lineageJson should contain key "executionEvent"
          lineageJson("executionPlan")("id") should equal(lineageJson("executionEvent")("planId"))
        }
      }
    }
  }

}
