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

import org.apache.spark.SPARK_VERSION
import org.apache.spark.sql.catalyst.expressions.Literal
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.io.TempDirectory
import za.co.absa.commons.scalatest.ConditionalTestTags.ignoreIf
import za.co.absa.commons.version.Version.VersionStringInterpolator
import za.co.absa.spline.test.fixture.spline.SplineFixture
import za.co.absa.spline.test.fixture.{SparkDatabaseFixture, SparkFixture}

/**
  *  Json DataSource V2 is not used by default in current Spark (3.1.x)
  *  This test is here to make sure we are ready for the Data Source V2 and as a starting point for future developments
  */
class JsonDSV2Spec extends AsyncFlatSpec
  with Matchers
  with SparkFixture
  with SplineFixture
  with SparkDatabaseFixture {

  val jsonDir: String = TempDirectory("SparkFixture", "UnitTestJson", pathOnly = true).deleteOnExit().path.toString.stripSuffix("/")

  it should "support json V2 command" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"3.0.0") in
    withCustomSparkSession(_
      .config("spark.sql.sources.useV1SourceList", "") // use V2 by default
    ) { implicit spark =>
      withLineageTracking { lineageCaptor =>
        val testData = {
          import spark.implicits._
          Seq((1014, "Warsaw"), (1002, "Corte")).toDF("ID", "NAME")
        }
        for {
          (plan1, Seq(event1)) <- lineageCaptor.lineageOf {
            testData.write.json(s"$jsonDir/jsonV2")
          }
          (plan2, Seq(event2)) <- lineageCaptor.lineageOf {
            val df = spark.read.json(s"$jsonDir/jsonV2")
            df.write.json(s"$jsonDir/jsonV2-2")
          }
        } yield {
          plan1.id.get shouldEqual event1.planId
          plan1.operations.write.append shouldBe false
          plan1.operations.write.extra.get("destinationType") shouldBe Some("json")
          plan1.operations.write.outputSource shouldBe s"file:$jsonDir/jsonV2"

          plan2.operations.reads.get(0).inputSources.head shouldBe s"file:$jsonDir/jsonV2"
        }
      }
    }



}
