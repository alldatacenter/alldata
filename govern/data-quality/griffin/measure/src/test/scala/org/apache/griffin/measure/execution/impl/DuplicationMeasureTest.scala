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

import org.apache.griffin.measure.configuration.dqdefinition.MeasureParam
import org.apache.griffin.measure.execution.Measure._
import org.apache.griffin.measure.execution.impl.DuplicationMeasure._

class DuplicationMeasureTest extends MeasureTest {
  var param: MeasureParam = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    param = MeasureParam(
      "param",
      "Duplication",
      "source",
      Map(Expression -> "name", BadRecordDefinition -> "duplicate"))
  }

  "DuplicationMeasure" should "validate expression config" in {
    // Validations for Duplication Expr

    // Empty
    assertThrows[AssertionError] {
      DuplicationMeasure(spark, param.copy(config = Map.empty[String, String]))
    }

    // Empty
    assertThrows[AssertionError] {
      DuplicationMeasure(spark, param.copy(config = Map(Expression -> StringUtils.EMPTY)))
    }

    // Null
    assertThrows[AssertionError] {
      DuplicationMeasure(spark, param.copy(config = Map(Expression -> null)))
    }

    // Incorrect Type
    assertThrows[AssertionError] {
      DuplicationMeasure(spark, param.copy(config = Map(Expression -> 1234)))
    }

    // Validations for BadRecordDefinition

    // Empty
    assertThrows[AssertionError] {
      DuplicationMeasure(spark, param.copy(config = Map(Expression -> "name")))
    }

    // Empty
    assertThrows[AssertionError] {
      DuplicationMeasure(
        spark,
        param.copy(config = Map(Expression -> "name", BadRecordDefinition -> "")))
    }

    // Null
    assertThrows[AssertionError] {
      DuplicationMeasure(
        spark,
        param.copy(config = Map(Expression -> "name", BadRecordDefinition -> null)))
    }

    // Incorrect Type
    assertThrows[AssertionError] {
      DuplicationMeasure(
        spark,
        param.copy(config = Map(Expression -> "name", BadRecordDefinition -> 435)))
    }

    // Incorrect Value
    assertThrows[AssertionError] {
      DuplicationMeasure(
        spark,
        param.copy(config = Map(Expression -> "name", BadRecordDefinition -> "xyz")))
    }
  }

  it should "support metric writing" in {
    val measure = DuplicationMeasure(spark, param)
    assertResult(true)(measure.supportsMetricWrite)
  }

  it should "support record writing" in {
    val measure = DuplicationMeasure(spark, param)
    assertResult(true)(measure.supportsRecordWrite)
  }

  it should "execute defined measure expr" in {
    val measure =
      DuplicationMeasure(spark, param.copy(config = Map(BadRecordDefinition -> "duplicate")))
    val (recordsDf, metricsDf) = measure.execute(source)

    assertResult(recordDfSchema)(recordsDf.schema)
    assertResult(metricDfSchema)(metricsDf.schema)

    assertResult(source.count())(recordsDf.count())
    assertResult(1L)(metricsDf.count())

    val metricMap = metricsDf
      .head()
      .getAs[Seq[Map[String, String]]](Metrics)
      .map(x => x(MetricName) -> x(MetricValue))
      .toMap

    assertResult(metricMap(Duplicate))("0")
    assertResult(metricMap(Unique))("5")
    assertResult(metricMap(NonUnique))("0")
    assertResult(metricMap(Distinct))("5")
    assertResult(metricMap(Total))("5")
  }

  it should "support complex measure expr" in {
    val measure = DuplicationMeasure(
      spark,
      param.copy(config = Map(Expression -> "name", BadRecordDefinition -> "duplicate")))
    val (recordsDf, metricsDf) = measure.execute(source)

    assertResult(recordDfSchema)(recordsDf.schema)
    assertResult(metricDfSchema)(metricsDf.schema)

    assertResult(source.count())(recordsDf.count())
    assertResult(1L)(metricsDf.count())

    val metricMap = metricsDf
      .head()
      .getAs[Seq[Map[String, String]]](Metrics)
      .map(x => x(MetricName) -> x(MetricValue))
      .toMap

    assertResult(metricMap(Duplicate))("1")
    assertResult(metricMap(Unique))("2")
    assertResult(metricMap(NonUnique))("1")
    assertResult(metricMap(Distinct))("3")
    assertResult(metricMap(Total))("5")
  }

  it should "support columns with space in their names" in {
    val newSource = source.withColumnRenamed("name", "full name")
    newSource.createOrReplaceTempView("newSource")
    val measure = DuplicationMeasure(
      spark,
      param.copy(
        dataSource = "newSource",
        config = Map(Expression -> "full name", BadRecordDefinition -> "duplicate")))

    val (recordsDf, metricsDf) = measure.execute(newSource)

    assertResult(newSource.schema.add(Status, "string", nullable = false))(recordsDf.schema)
    assertResult(metricDfSchema)(metricsDf.schema)

    assertResult(newSource.count())(recordsDf.count())
    assertResult(1L)(metricsDf.count())

    val metricMap = metricsDf
      .head()
      .getAs[Seq[Map[String, String]]](Metrics)
      .map(x => x(MetricName) -> x(MetricValue))
      .toMap

    assertResult(metricMap(Duplicate))("1")
    assertResult(metricMap(Unique))("2")
    assertResult(metricMap(NonUnique))("1")
    assertResult(metricMap(Distinct))("3")
    assertResult(metricMap(Total))("5")
  }

}
