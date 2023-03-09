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

package org.apache.griffin.measure.datasource.connector

import scala.util.Try

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.InputDStream
import org.scalatest.flatspec.AnyFlatSpec

import org.apache.griffin.measure.configuration.dqdefinition.DataConnectorParam
import org.apache.griffin.measure.context.TimeRange
import org.apache.griffin.measure.datasource.TimestampStorage
import org.apache.griffin.measure.datasource.cache.StreamingCacheClient
import org.apache.griffin.measure.datasource.connector.batch.{
  BatchDataConnector,
  MySqlDataConnector
}
import org.apache.griffin.measure.datasource.connector.streaming.{
  KafkaStreamingStringDataConnector,
  StreamingDataConnector
}

case class ExampleBatchDataConnector(
    @transient sparkSession: SparkSession,
    dcParam: DataConnectorParam,
    timestampStorage: TimestampStorage)
    extends BatchDataConnector {

  override def data(ms: Long): (Option[DataFrame], TimeRange) = (None, TimeRange(ms))
}

case class ExampleStreamingDataConnector(
    @transient sparkSession: SparkSession,
    @transient ssc: StreamingContext,
    dcParam: DataConnectorParam,
    timestampStorage: TimestampStorage,
    streamingCacheClientOpt: Option[StreamingCacheClient])
    extends StreamingDataConnector {
  override type K = Unit
  override type V = Unit
  override type OUT = Unit

  override protected def stream(): Try[InputDStream[this.OUT]] = null

  override def transform(rdd: RDD[this.OUT]): Option[DataFrame] = None

  override def init(): Unit = ()
}

class NotDataConnector

class DataConnectorWithoutApply extends BatchDataConnector {
  override val sparkSession: SparkSession = null
  override val dcParam: DataConnectorParam = null
  override val timestampStorage: TimestampStorage = null

  override def data(ms: Long): (Option[DataFrame], TimeRange) = null
}

class DataConnectorFactorySpec extends AnyFlatSpec {

  "DataConnectorFactory" should "be able to create custom batch connector" in {
    val param = DataConnectorParam(
      "CUSTOM",
      null,
      Map("class" -> classOf[ExampleBatchDataConnector].getCanonicalName),
      Nil)
    // apparently Scalamock can not mock classes without empty-paren constructor, providing nulls
    val res = DataConnectorFactory.getDataConnector(null, null, param, null, None)
    assert(res.get != null)
    assert(res.isSuccess)
    assert(res.get.isInstanceOf[ExampleBatchDataConnector])
    assert(res.get.data(42)._2.begin == 42)
  }

  it should "be able to create MySqlDataConnector" in {
    val param = DataConnectorParam(
      "CUSTOM",
      null,
      Map("class" -> classOf[MySqlDataConnector].getCanonicalName),
      Nil)
    // apparently Scalamock can not mock classes without empty-paren constructor, providing nulls
    val res = DataConnectorFactory.getDataConnector(null, null, param, null, None)
    assert(res.isSuccess)
    assert(res.get.isInstanceOf[MySqlDataConnector])
  }

  it should "be able to create KafkaStreamingStringDataConnector" in {
    val param = DataConnectorParam(
      "CUSTOM",
      null,
      Map("class" -> classOf[KafkaStreamingStringDataConnector].getCanonicalName),
      Nil)
    val res = DataConnectorFactory.getDataConnector(null, null, param, null, None)
    assert(res.isSuccess)
    assert(res.get.isInstanceOf[KafkaStreamingStringDataConnector])
  }

  it should "fail if class is not extending DataConnectors" in {
    val param = DataConnectorParam(
      "CUSTOM",
      null,
      Map("class" -> classOf[NotDataConnector].getCanonicalName),
      Nil)
    // apparently Scalamock can not mock classes without empty-paren constructor, providing nulls
    val res = DataConnectorFactory.getDataConnector(null, null, param, null, None)
    assert(res.isFailure)
    assert(res.failed.get.isInstanceOf[ClassCastException])
    assert(
      res.failed.get.getMessage ==
        "org.apache.griffin.measure.datasource.connector.NotDataConnector" +
          " should extend BatchDataConnector or StreamingDataConnector")
  }

  it should "fail if class does not have apply() method" in {
    val param = DataConnectorParam(
      "CUSTOM",
      null,
      Map("class" -> classOf[DataConnectorWithoutApply].getCanonicalName),
      Nil)
    // apparently Scalamock can not mock classes without empty-paren constructor, providing nulls
    val res = DataConnectorFactory.getDataConnector(null, null, param, null, None)
    assert(res.isFailure)
    assert(res.failed.get.isInstanceOf[NoSuchMethodException])
    assert(
      res.failed.get.getMessage ==
        "org.apache.griffin.measure.datasource.connector.DataConnectorWithoutApply.apply" +
          "(org.apache.spark.sql.SparkSession, " +
          "org.apache.griffin.measure.configuration.dqdefinition.DataConnectorParam, " +
          "org.apache.griffin.measure.datasource.TimestampStorage)")
  }

}
