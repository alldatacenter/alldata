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

import scala.util.{Failure, Success, Try}

import kafka.serializer.Decoder
import org.apache.spark.streaming.dstream.InputDStream

import org.apache.griffin.measure.utils.ParamUtil._

/**
 * streaming data connector for kafka
 */
trait KafkaStreamingDataConnector extends StreamingDataConnector {

  type KD <: Decoder[K]
  type VD <: Decoder[V]
  type OUT = (K, V)

  val config: Map[String, Any] = dcParam.getConfig

  val KafkaConfig = "kafka.config"
  val Topics = "topics"

  val kafkaConfig: Map[String, String] = config.getAnyRef(KafkaConfig, Map[String, String]())
  val topics: String = config.getString(Topics, "")

  def init(): Unit = {
    // register fan in
    streamingCacheClientOpt.foreach(_.registerFanIn())

    val ds = stream() match {
      case Success(dstream) => dstream
      case Failure(ex) => throw ex
    }
    ds.foreachRDD((rdd, time) => {
      val ms = time.milliseconds
      val saveDfOpt =
        try {
          // coalesce partition number
          val prlCount = rdd.sparkContext.defaultParallelism
          val ptnCount = rdd.getNumPartitions
          val repartitionedRdd = if (prlCount < ptnCount) {
            rdd.coalesce(prlCount)
          } else rdd

          val dfOpt = transform(repartitionedRdd)

          // pre-process
          preProcess(dfOpt, ms)
        } catch {
          case e: Throwable =>
            error(s"streaming data connector error: ${e.getMessage}")
            None
        }

      // save data frame
      streamingCacheClientOpt.foreach(_.saveData(saveDfOpt, ms))
    })
  }

  def stream(): Try[InputDStream[OUT]] = Try {
    val topicSet = topics.split(",").toSet
    createDStream(topicSet)
  }

  protected def createDStream(topicSet: Set[String]): InputDStream[OUT]

}
