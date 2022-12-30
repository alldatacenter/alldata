/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.connector.spark

import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream

import TubeMQFunctions._

class TubeMQProvider(val ssc: StreamingContext) {

  /**
   * Receive from tube
   * @param config the configuration of receiver's consumer
   * @param numReceiver the number of receivers
   * @return DStream[ Array[Byte] ]
   */
  def bytesStream(
      config: ConsumerConf,
      numReceiver: Int): DStream[Array[Byte]] = {
    require(numReceiver >= 1, s"the argument 'numReceiver' error: $numReceiver >= 1 ?")
    val streams = config match {
      case conf: TubeMQConsumerConf =>
        (1 to numReceiver).map { _ =>
          ssc.receiverStream(new TubeMQConsumer(conf))
        }
      case _ =>
        throw new UnsupportedOperationException("Unknown receiver config.")
    }
    ssc.union(streams)
  }

  /**
   * Receive from tube
   * @param config the configuration of receiver's consumer
   * @param numReceiver the number of receivers
   * @return DStream[String]
   */
  def textStream(
      config: ConsumerConf,
      numReceiver: Int): DStream[String] = {
    bytesStream(config, numReceiver).map(x => new String(x, "utf8"))
  }

  /**
   * Send to tube
   * @param dstream the data to be send
   * @param config sender config
   */
  def saveBytesStreamToTDBank(dstream: DStream[Array[Byte]], config: ProducerConf): Unit = {
    dstream.saveToTube(config)
  }

  /**
   * Send to tube
   * @param dstream the data to be send
   * @param config sender config
   */
  def saveTextStreamToTDBank(dstream: DStream[String], config: ProducerConf): Unit = {
    dstream.saveToTube(config)
  }
}

