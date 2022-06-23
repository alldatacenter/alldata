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


import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row, SaveMode}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import pl.allegro.tech.embeddedelasticsearch.{EmbeddedElastic, IndexSettings, PopularProperties}
import za.co.absa.commons.io.TempDirectory
import za.co.absa.commons.scalatest.ConditionalTestTags.ignoreIf
import za.co.absa.commons.version.Version._
import za.co.absa.spline.test.fixture.SparkFixture
import za.co.absa.spline.test.fixture.spline.SplineFixture

import java.util.concurrent.TimeUnit


class ElasticSearchSpec
  extends AsyncFlatSpec
    with Matchers
    with SparkFixture
    with SplineFixture {

  val index = "test"
  val docType = "test"
  val esNodes = "localhost"
  val tcpPort = 9350
  val clusterName = "my_cluster"
  val esOptions = Map("es.nodes" -> esNodes)

  it should "support ES" taggedAs ignoreIf(semver"${util.Properties.versionNumberString}" >= semver"2.12.0") in {

    val embeddedElastic = EmbeddedElastic.builder()
      .withElasticVersion("6.6.0")
      .withSetting(PopularProperties.TRANSPORT_TCP_PORT, tcpPort)
      .withSetting(PopularProperties.CLUSTER_NAME, clusterName)
      .withStartTimeout(5, TimeUnit.MINUTES)
      .withIndex(index, IndexSettings.builder().build())
      .build()
    embeddedElastic.start()

    withNewSparkSession(implicit spark => {
      withLineageTracking { captor =>

        val testData: DataFrame = {
          val schema = StructType(StructField("id", IntegerType, nullable = false) :: StructField("name", StringType, nullable = false) :: Nil)
          val rdd = spark.sparkContext.parallelize(Row(1014, "Warsaw") :: Row(1002, "Corte") :: Nil)
          spark.sqlContext.createDataFrame(rdd, schema)
        }

        for {
          (plan1, _) <- captor.lineageOf {
            testData
              .write
              .mode(SaveMode.Append)
              .options(esOptions)
              .format("es")
              .save(s"$index/$docType")
          }

          (plan2, _) <- captor.lineageOf {
            val df = spark
              .read
              .options(esOptions)
              .format("es")
              .load(s"$index/$docType")

            df.write.save(TempDirectory(pathOnly = true).deleteOnExit().path.toString)
          }
        } yield {
          embeddedElastic.stop()

          plan1.operations.write.append shouldBe true
          plan1.operations.write.extra.get("destinationType") shouldBe Some("elasticsearch")
          plan1.operations.write.outputSource shouldBe s"elasticsearch://$esNodes/$index/$docType"

          plan2.operations.reads.get.head.inputSources.head shouldBe plan1.operations.write.outputSource
          plan2.operations.reads.get.head.extra.get("sourceType") shouldBe Some("elasticsearch")
          plan2.operations.write.append shouldBe false
        }
      }
    })
  }
}
