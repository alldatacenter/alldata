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

import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.spark.sql.SaveMode.Overwrite
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.io.TempFile
import za.co.absa.spline.test.fixture.SparkFixture
import za.co.absa.spline.test.fixture.spline.SplineFixture

class KafkaSinkSpec
  extends AsyncFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with SparkFixture
    with SplineFixture
    with EmbeddedKafka {

  private var kafkaUrl = ""

  override def beforeAll: Unit = {
    implicit val config = EmbeddedKafkaConfig(0, 0)

    val kafka = EmbeddedKafka.start()
    kafkaUrl = s"localhost:${kafka.config.kafkaPort}"
  }

  override def afterAll: Unit = {
    EmbeddedKafka.stop()
  }

  it should "support Kafka as a write source" in {
    val topicName = "bananas"

    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        val testData: DataFrame = {
          val schema = StructType(StructField("ID", IntegerType, nullable = false) :: StructField("NAME", StringType, nullable = false) :: Nil)
          val rdd = spark.sparkContext.parallelize(Row(1014, "Warsaw") :: Row(1002, "Corte") :: Nil)
          spark.sqlContext.createDataFrame(rdd, schema)
        }

        def reader = spark
          .read
          .format("kafka")
          .option("kafka.bootstrap.servers", kafkaUrl)

        for {
          (plan1, _) <- captor.lineageOf(
            testData
              .selectExpr("CAST (NAME as STRING) as value")
              .write
              .format("kafka")
              .option("kafka.bootstrap.servers", kafkaUrl)
              .option("topic", topicName)
              .save())

          (plan2, _) <- captor.lineageOf(
            reader
              .option("subscribe", s"$topicName,anotherTopic")
              .load()
              .write.mode(Overwrite).save(TempFile(pathOnly = true).deleteOnExit().path.toString))

          (plan3, _) <- captor.lineageOf(
            reader
              .option("subscribePattern", ".*")
              .load()
              .write.mode(Overwrite).save(TempFile(pathOnly = true).deleteOnExit().path.toString))

          (plan4, _) <- captor.lineageOf(
            reader
              .option("assign", s"""{"$topicName":[0]}""")
              .load()
              .write.mode(Overwrite).save(TempFile(pathOnly = true).deleteOnExit().path.toString))
        } yield {
          plan1.operations.write.append shouldBe false
          plan1.operations.write.extra.get("destinationType") shouldBe Some("kafka")
          plan1.operations.write.outputSource shouldBe s"kafka:$topicName"

          plan2.operations.reads.get.head.extra.get("sourceType") shouldBe Some("kafka")
          plan2.operations.reads.get.head.inputSources should contain(s"kafka:$topicName")
          plan2.operations.reads.get.head.params.get should contain key "subscribe"

          plan3.operations.reads.get.head.extra.get("sourceType") shouldBe Some("kafka")
          plan3.operations.reads.get.head.inputSources should contain(s"kafka:$topicName")
          plan3.operations.reads.get.head.params.get should contain key "subscribepattern"

          plan4.operations.reads.get.head.extra.get("sourceType") shouldBe Some("kafka")
          plan4.operations.reads.get.head.inputSources should contain(s"kafka:$topicName")
          plan4.operations.reads.get.head.params.get should contain key "assign"
        }
      }
    }
  }
}
