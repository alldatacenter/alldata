/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.griffin.measure.sink

import scala.collection.mutable

import org.apache.spark.sql.functions._

import org.apache.griffin.measure.configuration.dqdefinition._
import org.apache.griffin.measure.configuration.enums.FlattenType.DefaultFlattenType
import org.apache.griffin.measure.configuration.enums.MeasureTypes
import org.apache.griffin.measure.execution.Measure._
import org.apache.griffin.measure.step.write.{MetricFlushStep, MetricWriteStep, RecordWriteStep}
class CustomSinkTest extends SinkTestBase {

  val sinkParam: SinkParam =
    SinkParam(
      "customSink",
      "custom",
      Map("class" -> "org.apache.griffin.measure.sink.CustomSink"))
  override var sinkParams = Seq(sinkParam)

  def withCustomSink[A](func: Iterable[Sink] => A): A = {
    val sinkFactory = SinkFactory(sinkParams, "Test Sink Factory")
    val timestamp = System.currentTimeMillis
    val sinks = sinkFactory.getSinks(timestamp, block = true)
    func(sinks)
  }

  "custom sink" can "sink metrics" in {
    val measureName = "test_measure"
    withCustomSink(sinks => {
      sinks.foreach { sink =>
        try {
          sink.sinkMetrics(
            Map(
              MeasureName -> measureName,
              Metrics -> Seq(Map(MetricName -> "sum", MetricValue -> "10"))))
        } catch {
          case e: Throwable => error(s"sink metrics error: ${e.getMessage}", e)
        }
      }

      sinks.foreach { sink =>
        try {
          sink.sinkMetrics(
            Map(
              MeasureName -> measureName,
              Metrics -> Seq(Map(MetricName -> "count", MetricValue -> "5"))))
        } catch {
          case e: Throwable => error(s"sink metrics error: ${e.getMessage}", e)
        }
      }
    })

    val actualMetricsOpt = CustomSinkResultRegister.getMetrics(measureName)
    assert(actualMetricsOpt.isDefined)

    val expected = Map("sum" -> "10", "count" -> "5")
    actualMetricsOpt.get should contain theSameElementsAs expected
  }

  "custom sink" can "sink records" in {
    val actualRecords = withCustomSink(sinks => {
      val rdd1 = createDataFrame(1 to 2)
      sinks.foreach { sink =>
        try {
          sink.sinkRecords(rdd1.toJSON.rdd, "test records")
        } catch {
          case e: Throwable => error(s"sink records error: ${e.getMessage}", e)
        }
      }
      val rdd2 = createDataFrame(2 to 4)
      sinks.foreach { sink =>
        try {
          sink.sinkRecords(rdd2.toJSON.rdd, "test records")
        } catch {
          case e: Throwable => error(s"sink records error: ${e.getMessage}", e)
        }
      }
      sinks.headOption match {
        case Some(sink: CustomSink) => sink.allRecords
        case _ =>
      }
    })

    val expected = List(
      "{\"id\":1,\"name\":\"name_1\",\"sex\":\"women\",\"age\":16}",
      "{\"id\":2,\"name\":\"name_2\",\"sex\":\"man\",\"age\":17}",
      "{\"id\":2,\"name\":\"name_2\",\"sex\":\"man\",\"age\":17}",
      "{\"id\":3,\"name\":\"name_3\",\"sex\":\"women\",\"age\":18}",
      "{\"id\":4,\"name\":\"name_4\",\"sex\":\"man\",\"age\":19}")

    actualRecords should be(expected)
  }

  val metricsDefaultOutput: RuleOutputParam =
    RuleOutputParam("metrics", "default_output", "default")

  "RecordWriteStep" should "work with custom sink" in {
    val resultTable = "result_table"
    val df = createDataFrame(1 to 5)
    df.createOrReplaceTempView(resultTable)

    val rwName = Some(metricsDefaultOutput).flatMap(_.getNameOpt).getOrElse(resultTable)
    val dQContext = getDqContext()
    RecordWriteStep(rwName, resultTable).execute(dQContext)

    val actualRecords = dQContext.getSinks.headOption match {
      case Some(sink: CustomSink) => sink.allRecords
      case _ => mutable.ListBuffer[String]()
    }

    val expected = List(
      "{\"id\":1,\"name\":\"name_1\",\"sex\":\"women\",\"age\":16}",
      "{\"id\":2,\"name\":\"name_2\",\"sex\":\"man\",\"age\":17}",
      "{\"id\":3,\"name\":\"name_3\",\"sex\":\"women\",\"age\":18}",
      "{\"id\":4,\"name\":\"name_4\",\"sex\":\"man\",\"age\":19}",
      "{\"id\":5,\"name\":\"name_5\",\"sex\":\"women\",\"age\":20}")

    actualRecords should be(expected)
  }

  val metricsEntriesOutput: RuleOutputParam =
    RuleOutputParam("metrics", "entries_output", "entries")
  val metricsArrayOutput: RuleOutputParam = RuleOutputParam("metrics", "array_output", "array")
  val metricsMapOutput: RuleOutputParam = RuleOutputParam("metrics", "map_output", "map")

  "MetricWriteStep" should "output default metrics with custom sink" in {
    val resultTable = "result_table"
    val df = createDataFrame(1 to 5)

    val metricCols =
      Seq("sex", "max_age", "avg_age").map(c =>
        map(lit(MetricName), lit(c), lit(MetricValue), col(c)))

    val metricDf = df
      .groupBy("sex")
      .agg(max("age").as("max_age"), avg("age").as("avg_age"))
      .select(array(metricCols: _*).as("metrics"))
      .select("metrics")

    metricDf.createOrReplaceTempView(resultTable)

    val dQContext = getDqContext()

    val metricWriteStep = MetricWriteStep("metrics", resultTable, DefaultFlattenType)

    val mp = MeasureParam(
      metricWriteStep.name,
      MeasureTypes.Profiling.toString,
      metricWriteStep.inputName)

    metricWriteStep.execute(dQContext)
    MetricFlushStep(Some(mp)).execute(dQContext)

    val expectedMetrics = Array(
      Map("metric_name" -> "sex", "metric_value" -> "man"),
      Map("metric_name" -> "max_age", "metric_value" -> "19"),
      Map("metric_name" -> "avg_age", "metric_value" -> "18.0"))

    val actualMetricsOpt = CustomSinkResultRegister.getMetrics(metricWriteStep.name)
    assert(actualMetricsOpt.isDefined)

    val actualMetricsMap: Map[String, Any] = actualMetricsOpt.get
    assert(actualMetricsMap.contains("metrics"))

    val actualMetrics = actualMetricsMap("metrics").asInstanceOf[Seq[Map[String, String]]]
    actualMetrics should contain theSameElementsAs expectedMetrics
  }

}
