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

package org.apache.griffin.measure.datasource.connector.streaming

import kafka.serializer.StringDecoder
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, _}
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka.KafkaUtils

import org.apache.griffin.measure.configuration.dqdefinition.DataConnectorParam
import org.apache.griffin.measure.datasource.TimestampStorage
import org.apache.griffin.measure.datasource.cache.StreamingCacheClient

/**
 * streaming data connector for kafka with string format key and value
 */
case class KafkaStreamingStringDataConnector(
    @transient sparkSession: SparkSession,
    @transient ssc: StreamingContext,
    dcParam: DataConnectorParam,
    timestampStorage: TimestampStorage,
    streamingCacheClientOpt: Option[StreamingCacheClient])
    extends KafkaStreamingDataConnector {

  type K = String
  type KD = StringDecoder
  type V = String
  type VD = StringDecoder

  val valueColName = "value"
  val schema: StructType = StructType(Array(StructField(valueColName, StringType)))

  def createDStream(topicSet: Set[String]): InputDStream[OUT] = {
    KafkaUtils.createDirectStream[K, V, KD, VD](ssc, kafkaConfig, topicSet)
  }

  def transform(rdd: RDD[OUT]): Option[DataFrame] = {
    if (rdd.isEmpty) None
    else {
      try {
        val rowRdd = rdd.map(d => Row(d._2))
        val df = sparkSession.createDataFrame(rowRdd, schema)
        Some(df)
      } catch {
        case _: Throwable =>
          error("streaming data transform fails")
          None
      }
    }
  }

}
