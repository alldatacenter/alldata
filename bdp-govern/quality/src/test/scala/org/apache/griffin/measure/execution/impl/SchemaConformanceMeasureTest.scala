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
import org.apache.spark.sql.functions.lit

import org.apache.griffin.measure.configuration.dqdefinition.MeasureParam
import org.apache.griffin.measure.execution.Measure._
import org.apache.griffin.measure.execution.impl.CompletenessMeasure._

class SchemaConformanceMeasureTest extends MeasureTest {
  var param: MeasureParam = _

  final val SourceColStr: String = "source.col"
  final val DataTypeStr: String = "type"

  override def beforeAll(): Unit = {
    super.beforeAll()
    param = MeasureParam(
      "param",
      "SchemaConformance",
      "source",
      Map(Expression -> Seq(Map(SourceColStr -> "id", DataTypeStr -> "int"))))
  }

  "SchemaConformanceMeasure" should "validate expression config" in {

    // Validations for SchemaConformance Expr

    // Empty
    assertThrows[AssertionError] {
      SchemaConformanceMeasure(spark, param.copy(config = Map.empty[String, String]))
    }

    // Incorrect Type and Empty
    assertThrows[AssertionError] {
      SchemaConformanceMeasure(spark, param.copy(config = Map(Expression -> StringUtils.EMPTY)))
    }

    // Null
    assertThrows[AssertionError] {
      SchemaConformanceMeasure(spark, param.copy(config = Map(Expression -> null)))
    }

    // Incorrect Type
    assertThrows[AssertionError] {
      SchemaConformanceMeasure(spark, param.copy(config = Map(Expression -> "gender")))
    }

    // Correct Type and Empty
    assertThrows[AssertionError] {
      SchemaConformanceMeasure(
        spark,
        param.copy(config = Map(Expression -> Seq.empty[Map[String, String]])))
    }

    // Invalid Expr
    assertThrows[AssertionError] {
      SchemaConformanceMeasure(
        spark,
        param.copy(config = Map(Expression -> Seq(Map("a" -> "b")))))
    }

    // Invalid Expr as type is missing
    assertThrows[AssertionError] {
      SchemaConformanceMeasure(
        spark,
        param.copy(config = Map(Expression -> Seq(Map(SourceColStr -> "b")))))
    }

    // Invalid Expr as source.col is missing
    assertThrows[AssertionError] {
      SchemaConformanceMeasure(
        spark,
        param.copy(config = Map(Expression -> Seq(Map(DataTypeStr -> "b")))))
    }
  }

  it should "support metric writing" in {
    val measure = SchemaConformanceMeasure(spark, param)
    assertResult(true)(measure.supportsMetricWrite)
  }

  it should "support record writing" in {
    val measure = SchemaConformanceMeasure(spark, param)
    assertResult(true)(measure.supportsRecordWrite)
  }

  it should "execute defined measure expr" in {
    val measure = SchemaConformanceMeasure(spark, param)
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
    assertResult(metricMap(Complete))("5")
    assertResult(metricMap(InComplete))("0")
  }

  it should "be able to check for date column" in {
    val newSource = source.withColumn("Test_date", lit("2009-07-30"))
    newSource.createOrReplaceTempView("newSource")
    val measure = SchemaConformanceMeasure(
      spark,
      param.copy(
        dataSource = "newSource",
        config = Map(Expression -> Seq(Map(SourceColStr -> "Test_date", DataTypeStr -> "date")))))
    val (recordsDf, metricsDf) = measure.execute(newSource)

    val newRecordDfSchema = newSource.schema.add(Status, "string", nullable = false)
    assertResult(newRecordDfSchema)(recordsDf.schema)
    assertResult(metricDfSchema)(metricsDf.schema)

    assertResult(newSource.count())(recordsDf.count())
    assertResult(1L)(metricsDf.count())

    val metricMap = metricsDf
      .head()
      .getAs[Seq[Map[String, String]]](Metrics)
      .map(x => x(MetricName) -> x(MetricValue))
      .toMap

    assertResult(metricMap(Total))("5")
    assertResult(metricMap(Complete))("5")
    assertResult(metricMap(InComplete))("0")
  }

  it should "be able to check for timestamp column and support space in column name" in {
    val newSource = source.withColumn("Test date", lit("2009-07-30"))
    newSource.createOrReplaceTempView("newSource")
    val measure = SchemaConformanceMeasure(
      spark,
      param.copy(
        dataSource = "newSource",
        config =
          Map(Expression -> Seq(Map(SourceColStr -> "Test date", DataTypeStr -> "timestamp")))))
    val (recordsDf, metricsDf) = measure.execute(newSource)

    val newRecordDfSchema = newSource.schema.add(Status, "string", nullable = false)
    assertResult(newRecordDfSchema)(recordsDf.schema)
    assertResult(metricDfSchema)(metricsDf.schema)

    assertResult(newSource.count())(recordsDf.count())
    assertResult(1L)(metricsDf.count())

    val metricMap = metricsDf
      .head()
      .getAs[Seq[Map[String, String]]](Metrics)
      .map(x => x(MetricName) -> x(MetricValue))
      .toMap

    assertResult(metricMap(Total))("5")
    assertResult(metricMap(Complete))("5")
    assertResult(metricMap(InComplete))("0")
  }

}
