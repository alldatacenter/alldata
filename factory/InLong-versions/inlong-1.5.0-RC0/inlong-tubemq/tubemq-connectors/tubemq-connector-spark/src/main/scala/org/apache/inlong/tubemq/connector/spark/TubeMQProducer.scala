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

import java.util.HashSet

import org.apache.spark.SparkException

import org.apache.inlong.tubemq.client.config.TubeClientConfig
import org.apache.inlong.tubemq.client.factory.TubeSingleSessionFactory
import org.apache.inlong.tubemq.client.producer.{MessageProducer, MessageSentCallback, MessageSentResult}
import org.apache.inlong.tubemq.corebase.Message
import org.slf4j.{Logger, LoggerFactory}

private[spark] class TubeMQProducer(
    topic: String,
    masterHostAndPort: String,
    maxRetryTimes: Int,
    exitOnException: Boolean)
  extends Serializable {

  private val LOG: Logger = LoggerFactory.getLogger(classOf[TubeMQProducer])

  private var producer: MessageProducer = _
  private var sessionFactory: TubeSingleSessionFactory = _

  def this(producerConfig: TubeMQProducerConf) = {
    this(producerConfig.topic, producerConfig.masterHostAndPort, producerConfig.maxRetryTimes, producerConfig.exitOnException)
  }

  def start(): Unit = {
    val clientConfig = new TubeClientConfig(masterHostAndPort)
    sessionFactory = new TubeSingleSessionFactory(clientConfig)
    producer = sessionFactory.createProducer()
    val hashSet: HashSet[String] = new HashSet[String]
    hashSet.add(topic)
    producer.publish(hashSet)
  }

  def send(body: Array[Byte]): Unit = {
    val message: Message = new Message(topic, body)
    producer.sendMessage(message, new MessageSentCallback {
      override def onMessageSent(sendResult: MessageSentResult): Unit = {
        if (!sendResult.isSuccess) {
          // rollback to sync
          sendSync(message)
        }
      }

      override def onException(throwable: Throwable): Unit = {
        if (exitOnException) {
          throw new SparkException("Sender message exception", throwable)
        } else {
          LOG.warn("Sender message exception", throwable)
        }
      }

    })
  }

  private def sendSync(message: Message): Unit = {
    var success = false
    var retryTimes = 0
    while (!success && retryTimes < maxRetryTimes) {
      retryTimes += 1
      val sendResult = producer.sendMessage(message)
      if (sendResult.isSuccess) {
        success = true
      }
      else {
        retryTimes = maxRetryTimes
      }
    }

    if (!success) {
      val error = s"Sender message exception: exceed maxRetryTimes($maxRetryTimes)"
      if (exitOnException) {
        throw new SparkException(error)
      } else {
        LOG.warn(error)
      }
    }
  }

  def close(): Unit = {
    try {
      if (producer != null) {
        producer.shutdown
        producer = null
      }
      if (sessionFactory != null) {
        sessionFactory.shutdown
        sessionFactory = null
      }
    }
    catch {
      case e: Exception => {
        LOG.error("Shutdown producer error", e)
      }
    }
  }

}
