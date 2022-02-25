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
import org.apache.spark.sql.catalyst.parser.ParseException

import org.apache.griffin.measure.configuration.dqdefinition.MeasureParam
import org.apache.griffin.measure.execution.Measure._
import org.apache.griffin.measure.execution.impl.CompletenessMeasure._

class CompletenessMeasureTest extends MeasureTest {
  var param: MeasureParam = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    param = MeasureParam("param", "Completeness", "source", Map(Expression -> "name is not null"))
  }

  "CompletenessMeasure" should "validate expression config" in {
    assertThrows[AssertionError] {
      CompletenessMeasure(spark, param.copy(config = Map.empty[String, String]))
    }

    assertThrows[AssertionError] {
      CompletenessMeasure(spark, param.copy(config = Map(Expression -> StringUtils.EMPTY)))
    }

    assertThrows[AssertionError] {
      CompletenessMeasure(spark, param.copy(config = Map(Expression -> null)))
    }

    assertThrows[AssertionError] {
      CompletenessMeasure(spark, param.copy(config = Map(Expression -> 22)))
    }
  }

  it should "support metric writing" in {
    val measure = CompletenessMeasure(spark, param)
    assertResult(true)(measure.supportsMetricWrite)
  }

  it should "support record writing" in {
    val measure = CompletenessMeasure(spark, param)
    assertResult(true)(measure.supportsRecordWrite)
  }

  it should "execute defined measure expr" in {
    val measure = new CompletenessMeasure(spark, param)
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
    assertResult(metricMap(Complete))("4")
    assertResult(metricMap(InComplete))("1")
  }

  it should "supported complex measure expr" in {
    val measure = new CompletenessMeasure(
      spark,
      param.copy(config = Map(Expression -> "name is not null and gender is not null")))
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
    assertResult(metricMap(Complete))("3")
    assertResult(metricMap(InComplete))("2")
  }

  it should "throw runtime error for invalid expr" in {
    assertThrows[AnalysisException] {
      new CompletenessMeasure(spark, param.copy(config = Map(Expression -> "xyz is null")))
        .execute(source)
    }

    assertThrows[ParseException] {
      new CompletenessMeasure(spark, param.copy(config = Map(Expression -> "select 1")))
        .execute(source)
    }
  }

}
