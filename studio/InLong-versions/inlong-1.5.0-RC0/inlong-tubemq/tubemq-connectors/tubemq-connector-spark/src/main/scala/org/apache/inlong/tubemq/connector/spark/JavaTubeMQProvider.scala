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

import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.api.java.{JavaDStream, JavaStreamingContext}

class JavaTubeMQProvider private(tubeProvider: TubeMQProvider) {
  def this(jssc: JavaStreamingContext) = {
    this(new TubeMQProvider(jssc.ssc))
  }

  def bytesStream(
      config: ConsumerConf,
      numReceiver: Int): JavaDStream[Array[Byte]] = {
    tubeProvider.bytesStream(config, numReceiver)
  }

  def textStream(
      config: ConsumerConf,
      numReceiver: Int,
      storageLevel: StorageLevel): JavaDStream[String] = {
    tubeProvider.textStream(config, numReceiver)
  }

  def saveBytesStreamToTDBank(
      dstream: JavaDStream[Array[Byte]],
      config: ProducerConf): Unit = {
    tubeProvider.saveBytesStreamToTDBank(dstream.dstream, config)
  }

  def saveTextStreamToTDBank(
      dstream: JavaDStream[String],
      config: ProducerConf): Unit = {
    tubeProvider.saveTextStreamToTDBank(dstream.dstream, config)
  }
}
