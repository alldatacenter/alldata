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
import org.apache.griffin.measure.execution.impl.AccuracyMeasure._

class AccuracyMeasureTest extends MeasureTest {
  var param: MeasureParam = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    param = MeasureParam(
      "param",
      "Accuracy",
      "source",
      Map(
        Expression -> Seq(Map(SourceColStr -> "gender", ReferenceColStr -> "gender")),
        ReferenceSourceStr -> "reference"))
  }

  "AccuracyMeasure" should "validate expression config" in {

    // Validations for Accuracy Expr

    // Empty
    assertThrows[AssertionError] {
      AccuracyMeasure(spark, param.copy(config = Map.empty[String, String]))
    }

    // Incorrect Type and Empty
    assertThrows[AssertionError] {
      AccuracyMeasure(spark, param.copy(config = Map(Expression -> StringUtils.EMPTY)))
    }

    // Null
    assertThrows[AssertionError] {
      AccuracyMeasure(spark, param.copy(config = Map(Expression -> null)))
    }

    // Incorrect Type
    assertThrows[AssertionError] {
      AccuracyMeasure(spark, param.copy(config = Map(Expression -> "gender")))
    }

    // Correct Type and Empty
    assertThrows[AssertionError] {
      AccuracyMeasure(
        spark,
        param.copy(config = Map(Expression -> Seq.empty[Map[String, String]])))
    }

    // Invalid Expr
    assertThrows[AssertionError] {
      AccuracyMeasure(
        spark,
        param.copy(config =
          Map(Expression -> Seq(Map("a" -> "b")), ReferenceSourceStr -> "reference")))
    }

    // Invalid Expr as ref.col is missing
    assertThrows[AssertionError] {
      AccuracyMeasure(
        spark,
        param.copy(config =
          Map(Expression -> Seq(Map(SourceColStr -> "b")), ReferenceSourceStr -> "reference")))
    }

    // Invalid Expr as source.col is missing
    assertThrows[AssertionError] {
      AccuracyMeasure(
        spark,
        param.copy(config =
          Map(Expression -> Seq(Map(ReferenceColStr -> "b")), ReferenceSourceStr -> "reference")))
    }

    // Validations for Reference source

    // Empty
    assertThrows[AssertionError] {
      AccuracyMeasure(
        spark,
        param.copy(config =
          Map(Expression -> Seq(Map("a" -> "b")), ReferenceSourceStr -> StringUtils.EMPTY)))
    }

    // Incorrect Type
    assertThrows[AssertionError] {
      AccuracyMeasure(
        spark,
        param.copy(config = Map(Expression -> Seq(Map("a" -> "b")), ReferenceSourceStr -> 2331)))
    }

    // Null
    assertThrows[AssertionError] {
      AccuracyMeasure(
        spark,
        param.copy(config = Map(Expression -> Seq(Map("a" -> "b")), ReferenceSourceStr -> null)))
    }

    // Invalid Reference
    assertThrows[AssertionError] {
      AccuracyMeasure(
        spark,
        param.copy(config = Map(Expression -> Seq(Map("a" -> "b")), ReferenceSourceStr -> "jj")))
    }
  }

  it should "support metric writing" in {
    val measure = AccuracyMeasure(spark, param)
    assertResult(true)(measure.supportsMetricWrite)
  }

  it should "support record writing" in {
    val measure = AccuracyMeasure(spark, param)
    assertResult(true)(measure.supportsRecordWrite)
  }

  it should "execute defined measure expr" in {
    val measure = AccuracyMeasure(spark, param)
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

    assertResult(metricMap(Total))("5")
    assertResult(metricMap(AccurateStr))("2")
    assertResult(metricMap(InAccurateStr))("3")
  }

  it should "support space in column name" in {
    val newSource = source.withColumnRenamed("name", "first name")
    newSource.createOrReplaceTempView("newSource")
    val measure = AccuracyMeasure(spark, param)
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

    assertResult(metricMap(Total))("5")
    assertResult(metricMap(AccurateStr))("2")
    assertResult(metricMap(InAccurateStr))("3")
  }

}
