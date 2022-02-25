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

package org.apache.griffin.measure.execution.impl

import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.AnalysisException

import org.apache.griffin.measure.configuration.dqdefinition.MeasureParam
import org.apache.griffin.measure.execution.Measure._
import org.apache.griffin.measure.execution.impl.CompletenessMeasure._

class SparkSqlMeasureTest extends MeasureTest {
  var param: MeasureParam = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    param = MeasureParam(
      "param",
      "SparkSql",
      "source",
      Map(
        Expression -> "select * from source",
        BadRecordDefinition -> "name is null or gender is null"))
  }

  "SparkSqlMeasure" should "validate expression config" in {

    // Validations for Expression

    // Empty
    assertThrows[AssertionError] {
      SparkSQLMeasure(spark, param.copy(config = Map.empty[String, String]))
    }

    // Empty
    assertThrows[AssertionError] {
      SparkSQLMeasure(spark, param.copy(config = Map(Expression -> StringUtils.EMPTY)))
    }

    // Null
    assertThrows[AssertionError] {
      SparkSQLMeasure(spark, param.copy(config = Map(Expression -> null)))
    }

    // Incorrect Type
    assertThrows[AssertionError] {
      SparkSQLMeasure(spark, param.copy(config = Map(Expression -> 943)))
    }

    // Validations for BadRecordDefinition

    // Empty
    assertThrows[AssertionError] {
      SparkSQLMeasure(
        spark,
        param.copy(config =
          Map(Expression -> "select 1", BadRecordDefinition -> StringUtils.EMPTY)))
    }

    // Incorrect Type
    assertThrows[AssertionError] {
      SparkSQLMeasure(
        spark,
        param.copy(config = Map(Expression -> "select 1", BadRecordDefinition -> 2344)))
    }

    // Null
    assertThrows[AssertionError] {
      SparkSQLMeasure(
        spark,
        param.copy(config = Map(Expression -> "select 1", BadRecordDefinition -> null)))
    }

  }

  it should "support metric writing" in {
    val measure = SparkSQLMeasure(spark, param)
    assertResult(true)(measure.supportsMetricWrite)
  }

  it should "support record writing" in {
    val measure = SparkSQLMeasure(spark, param)
    assertResult(true)(measure.supportsRecordWrite)
  }

  it should "execute defined measure expr" in {
    val measure = SparkSQLMeasure(spark, param)
    val (recordsDf, metricsDf) = measure.execute(source)

    assertResult(metricDfSchema)(metricsDf.schema)
    assertResult(source.count())(recordsDf.count())
    assertResult(1L)(metricsDf.count())

    val metricMap = metricsDf
      .head()
      .getAs[Seq[Map[String, String]]](Metrics)
      .map(x => x(MetricName) -> x(MetricValue))
      .toMap

    assertResult(metricMap(Total))("5")
    assertResult(metricMap(Complete))("3")
    assertResult(metricMap(InComplete))("2")
  }

  it should "throw runtime error for invalid expr" in {
    assertThrows[AssertionError] {
      SparkSQLMeasure(
        spark,
        param.copy(config =
          Map(Expression -> "select * from source", BadRecordDefinition -> "name")))
        .execute(source)
    }

    assertThrows[AnalysisException] {
      SparkSQLMeasure(
        spark,
        param.copy(config =
          Map(Expression -> "select 1 as my_value", BadRecordDefinition -> "name is null")))
        .execute(source)
    }
  }

}
