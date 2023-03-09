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
import scala.util.matching.Regex

import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.StreamingContext

import org.apache.griffin.measure.Loggable
import org.apache.griffin.measure.configuration.dqdefinition.DataConnectorParam
import org.apache.griffin.measure.datasource.TimestampStorage
import org.apache.griffin.measure.datasource.cache.StreamingCacheClient
import org.apache.griffin.measure.datasource.connector.batch._
import org.apache.griffin.measure.datasource.connector.streaming._

object DataConnectorFactory extends Loggable {

  @deprecated(
    s"This class is deprecated. Use '${classOf[FileBasedDataConnector].getCanonicalName}' with correct format.",
    "0.6.0")
  val AvroRegex: Regex = """^(?i)avro$""".r

  @deprecated(
    s"This class is deprecated. Use '${classOf[FileBasedDataConnector].getCanonicalName}' with correct format.",
    "0.6.0")
  val TextDirRegex: Regex = """^(?i)text-dir$""".r

  val HiveRegex: Regex = """^(?i)hive$""".r
  val FileRegex: Regex = """^(?i)file$""".r
  val KafkaRegex: Regex = """^(?i)kafka$""".r
  val JDBCRegex: Regex = """^(?i)jdbc$""".r
  val CustomRegex: Regex = """^(?i)custom$""".r
  val ElasticSearchRegex: Regex = """^(?i)elasticsearch$""".r

  /**
   * create data connector
   * @param sparkSession     spark env
   * @param ssc              spark streaming env
   * @param dcParam          data connector param
   * @param tmstCache        same tmst cache in one data source
   * @param streamingCacheClientOpt   for streaming cache
   * @return   data connector
   */
  def getDataConnector(
      sparkSession: SparkSession,
      ssc: StreamingContext,
      dcParam: DataConnectorParam,
      tmstCache: TimestampStorage,
      streamingCacheClientOpt: Option[StreamingCacheClient]): Try[DataConnector] = {
    val conType = dcParam.getType
    Try {
      conType match {
        case HiveRegex() => HiveBatchDataConnector(sparkSession, dcParam, tmstCache)
        case AvroRegex() => AvroBatchDataConnector(sparkSession, dcParam, tmstCache)
        case FileRegex() => FileBasedDataConnector(sparkSession, dcParam, tmstCache)
        case TextDirRegex() => TextDirBatchDataConnector(sparkSession, dcParam, tmstCache)
        case ElasticSearchRegex() => ElasticSearchDataConnector(sparkSession, dcParam, tmstCache)
        case CustomRegex() =>
          getCustomConnector(sparkSession, ssc, dcParam, tmstCache, streamingCacheClientOpt)
        case KafkaRegex() =>
          getStreamingDataConnector(
            sparkSession,
            ssc,
            dcParam,
            tmstCache,
            streamingCacheClientOpt)
        case JDBCRegex() => JDBCBasedDataConnector(sparkSession, dcParam, tmstCache)
        case _ => throw new Exception("connector creation error!")
      }
    }
  }

  private def getStreamingDataConnector(
      sparkSession: SparkSession,
      ssc: StreamingContext,
      dcParam: DataConnectorParam,
      tmstCache: TimestampStorage,
      streamingCacheClientOpt: Option[StreamingCacheClient]): StreamingDataConnector = {
    if (ssc == null) throw new Exception("streaming context is null!")
    val conType = dcParam.getType
    conType match {
      case KafkaRegex() =>
        getKafkaDataConnector(sparkSession, ssc, dcParam, tmstCache, streamingCacheClientOpt)
      case _ => throw new Exception("streaming connector creation error!")
    }
  }

  private def getCustomConnector(
      sparkSession: SparkSession,
      ssc: StreamingContext,
      dcParam: DataConnectorParam,
      timestampStorage: TimestampStorage,
      streamingCacheClientOpt: Option[StreamingCacheClient]): DataConnector = {
    val className = dcParam.getConfig("class").asInstanceOf[String]
    val cls = Class.forName(className)
    if (classOf[BatchDataConnector].isAssignableFrom(cls)) {
      val method = cls.getDeclaredMethod(
        "apply",
        classOf[SparkSession],
        classOf[DataConnectorParam],
        classOf[TimestampStorage])
      method
        .invoke(null, sparkSession, dcParam, timestampStorage)
        .asInstanceOf[BatchDataConnector]
    } else if (classOf[StreamingDataConnector].isAssignableFrom(cls)) {
      val method = cls.getDeclaredMethod(
        "apply",
        classOf[SparkSession],
        classOf[StreamingContext],
        classOf[DataConnectorParam],
        classOf[TimestampStorage],
        classOf[Option[StreamingCacheClient]])
      method
        .invoke(null, sparkSession, ssc, dcParam, timestampStorage, streamingCacheClientOpt)
        .asInstanceOf[StreamingDataConnector]
    } else {
      throw new ClassCastException(
        s"$className should extend BatchDataConnector or StreamingDataConnector")
    }
  }

  private def getKafkaDataConnector(
      sparkSession: SparkSession,
      ssc: StreamingContext,
      dcParam: DataConnectorParam,
      tmstCache: TimestampStorage,
      streamingCacheClientOpt: Option[StreamingCacheClient]): KafkaStreamingDataConnector = {
    val KeyType = "key.type"
    val ValueType = "value.type"
    val config = dcParam.getConfig
    val keyType = config.getOrElse(KeyType, "java.lang.String").toString
    val valueType = config.getOrElse(ValueType, "java.lang.String").toString

    (keyType, valueType) match {
      case ("java.lang.String", "java.lang.String") =>
        KafkaStreamingStringDataConnector(
          sparkSession,
          ssc,
          dcParam,
          tmstCache,
          streamingCacheClientOpt)
      case _ =>
        throw new Exception("not supported type kafka data connector")
    }
  }

}
