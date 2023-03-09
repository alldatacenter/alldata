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

package org.apache.griffin.measure.job

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

import org.apache.griffin.measure.Application.readParamFile
import org.apache.griffin.measure.configuration.dqdefinition.EnvConfig
import org.apache.griffin.measure.sink.CustomSinkResultRegister
import org.apache.griffin.measure.step.builder.udf.GriffinUDFAgent

class BatchDQAppTest extends DQAppTest {

  override def beforeAll(): Unit = {
    super.beforeAll()

    envParam = readParamFile[EnvConfig](getConfigFilePath("/env-batch.json")) match {
      case Success(p) => p
      case Failure(ex) =>
        error(ex.getMessage, ex)
        sys.exit(-2)
    }

    sparkParam = envParam.getSparkParam

    Try {
      sparkParam.getConfig.foreach { case (k, v) => spark.conf.set(k, v) }

      val logLevel = getGriffinLogLevel
      sc.setLogLevel(sparkParam.getLogLevel)
      griffinLogger.setLevel(logLevel)

      // register udf
      GriffinUDFAgent.register(spark)
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    dqApp = null
    CustomSinkResultRegister.clear()
  }

  override def afterEach(): Unit = {
    super.afterEach()

    dqApp = null
    CustomSinkResultRegister.clear()
  }

  // check Result Metrics
  def runAndCheckResult(expectedMetrics: Map[String, Map[String, Any]]): Unit = {
    val measureNames = dqApp.dqParam.getMeasures
    assert(measureNames.nonEmpty)

    measureNames.foreach(param => {
      val actualMetricsOpt = CustomSinkResultRegister.getMetrics(param.getName)
      assert(actualMetricsOpt.isDefined)

      val actualMetricsMap = actualMetricsOpt.get

      assert(expectedMetrics.contains(param.getName))
      actualMetricsMap should contain theSameElementsAs expectedMetrics(param.getName)
    })
  }

  def runAndCheckException[T <: AnyRef](implicit classTag: ClassTag[T]): Unit = {
    dqApp.run match {
      case Success(_) =>
        fail(
          s"job ${dqApp.dqParam.getName} should not succeed, a ${classTag.toString} exception is expected.")
      case Failure(ex) => assertThrows[T](throw ex)
    }
  }

  "accuracy batch job" should "work" in {
    dqApp = runApp("/_accuracy-batch-griffindsl.json")
    val expectedMetrics = Map("total" -> "50", "accurate" -> "45", "inaccurate" -> "5")

    runAndCheckResult(Map("accuracy_measure" -> expectedMetrics))
  }

  "completeness batch job" should "work" in {
    dqApp = runApp("/_completeness-batch-griffindsl.json")
    val expectedMetrics = Map("total" -> "50", "incomplete" -> "1", "complete" -> "49")

    runAndCheckResult(Map("completeness_measure" -> expectedMetrics))
  }

  "duplication batch job" should "work" in {
    dqApp = runApp("/_distinctness-batch-griffindsl.json")
    val expectedMetrics =
      Map(
        "duplicate" -> "1",
        "unique" -> "48",
        "non_unique" -> "1",
        "distinct" -> "49",
        "total" -> "50")

    runAndCheckResult(Map("duplication_measure" -> expectedMetrics))
  }

  "spark sql batch job" should "work" in {
    dqApp = runApp("/_sparksql-batch-griffindsl.json")

    val expectedMetrics =
      Map(
        "query_measure1" -> Map("total" -> "13", "complete" -> "13", "incomplete" -> "0"),
        "query_measure2" -> Map("total" -> "1", "complete" -> "0", "incomplete" -> "1"))

    runAndCheckResult(expectedMetrics)
  }

  "profiling batch job" should "work" in {
    dqApp = runApp("/_profiling-batch-griffindsl.json")

    val expectedMetrics = Map(
      "user_id" -> Map(
        "avg_col_len" -> "5.0",
        "max_col_len" -> "5",
        "variance" -> "15.17",
        "kurtosis" -> "-1.21",
        "avg" -> "10007.0",
        "min" -> "10001",
        "null_count" -> "0",
        "approx_distinct_count" -> "13",
        "total" -> "13",
        "std_dev" -> "3.89",
        "data_type" -> "bigint",
        "max" -> "10013",
        "min_col_len" -> "5"),
      "first_name" -> Map(
        "avg_col_len" -> "6.0",
        "max_col_len" -> "6",
        "variance" -> "0",
        "kurtosis" -> "0",
        "avg" -> "0",
        "min" -> "0",
        "null_count" -> "0",
        "approx_distinct_count" -> "13",
        "total" -> "13",
        "std_dev" -> "0",
        "data_type" -> "string",
        "max" -> "0",
        "min_col_len" -> "6"))

    runAndCheckResult(Map("profiling_measure" -> expectedMetrics))
  }

  "batch job" should "fail with exception caught due to invalid rules" in {
    assertThrows[org.apache.spark.sql.AnalysisException] {
      runApp("/_profiling-batch-griffindsl_malformed.json")
    }
  }

  "batch job" should "fail with exception when no measures or rules are defined" in {
    assertThrows[AssertionError] {
      runApp("/_no_measure_or_rules_malformed.json")
    }
  }
}
