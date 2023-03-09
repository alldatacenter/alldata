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

import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.types.{ArrayType, MapType, StringType, StructType}
import org.scalatest.matchers.should._

import org.apache.griffin.measure.execution.Measure._
import org.apache.griffin.measure.SparkSuiteBase
import org.apache.griffin.measure.configuration.enums.ProcessType.BatchProcessType
import org.apache.griffin.measure.context.{ContextId, DQContext}

trait MeasureTest extends SparkSuiteBase with Matchers {

  var sourceSchema: StructType = _
  var referenceSchema: StructType = _
  var recordDfSchema: StructType = _
  var metricDfSchema: StructType = _
  var context: DQContext = _

  var source: DataFrame = _
  var reference: DataFrame = _

  override def beforeAll(): Unit = {
    super.beforeAll()

    context =
      DQContext(ContextId(System.currentTimeMillis), "test-context", Nil, Nil, BatchProcessType)(
        spark)

    sourceSchema =
      new StructType().add("id", "integer").add("name", "string").add("gender", "string")

    referenceSchema = new StructType().add("gender", "string")

    recordDfSchema = sourceSchema.add(Status, "string", nullable = false)
    metricDfSchema = new StructType()
      .add(
        Metrics,
        ArrayType(MapType(StringType, StringType), containsNull = false),
        nullable = false)

    source = spark
      .createDataset(
        Seq(
          Row(1, "John Smith", "Male"),
          Row(2, "John Smith", null),
          Row(3, "Rebecca Davis", "Female"),
          Row(4, "Paul Adams", "Male"),
          Row(5, null, null)))(RowEncoder(sourceSchema))
      .cache()

    reference = spark.createDataset(Seq(Row("Male")))(RowEncoder(referenceSchema)).cache()

    source.createOrReplaceTempView("source")
    reference.createOrReplaceTempView("reference")
  }

}
