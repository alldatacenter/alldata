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

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkEnv
import org.apache.spark.streaming.Time
import org.apache.spark.streaming.dstream.DStream

import scala.language.implicitConversions

object TubeMQFunctions {

  class ArrayByteRDDFunctins(rdd: RDD[Array[Byte]]) extends Serializable {
    def saveToTube(config: ProducerConf): Unit = {
      SparkEnv.get.conf.set("spark.task.maxFailures", "1")
      SparkEnv.get.conf.set("spark.speculation", "false")
      config match {
        case conf: TubeMQProducerConf =>
          val sender = new TubeMQProducer(conf)
          val writer = (iter: Iterator[Array[Byte]]) => {
            sender.start()
            try {
              while (iter.hasNext) {
                val record = iter.next()
                sender.send(record)
              }
            } finally {
              sender.close()
            }
          }
          rdd.context.runJob(rdd, writer)

        case _ => throw new UnsupportedOperationException("Unknown SenderConfig")
      }
    }
  }

  class StringRDDFunctins(rdd: RDD[String]) extends Serializable {
    def saveToTube(config: ProducerConf): Unit = {
      rdd.map(_.getBytes("UTF-8")).saveToTube(config)
    }
  }

  class ArrayByteDStreamFunctins(dstream: DStream[Array[Byte]]) extends Serializable {
    def saveRDD(rdd: RDD[Array[Byte]], sender: TubeMQProducer): Unit = {
      val writer = (iter: Iterator[Array[Byte]]) => {
        try {
          sender.start()
          while (iter.hasNext) {
            val record = iter.next()
            sender.send(record)
          }
        }
        finally {
          sender.close()
        }
      }
      rdd.context.runJob(rdd, writer)
    }

    def saveToTube(config: ProducerConf): Unit = {
      SparkEnv.get.conf.set("spark.task.maxFailures", "1")
      SparkEnv.get.conf.set("spark.speculation", "false")
      val sender: TubeMQProducer = config match {
        case conf: TubeMQProducerConf => new TubeMQProducer(conf)
        case _ => throw new UnsupportedOperationException("Unknown SenderConfig")
      }
      val saveFunc = (rdd: RDD[Array[Byte]], time: Time) => {
        saveRDD(rdd, sender)
      }
      dstream.foreachRDD(saveFunc)
    }
  }

  class StringDStreamFunctins(dstream: DStream[String]) extends Serializable {
    def saveToTube(config: ProducerConf): Unit = {
      dstream.map(_.getBytes("UTF-8")).saveToTube(config)
    }
  }

  implicit def rddToArrayByteRDDFunctions(rdd: RDD[Array[Byte]]): ArrayByteRDDFunctins = {
    new ArrayByteRDDFunctins(rdd)
  }

  implicit def rddToStringRDDFunctions(rdd: RDD[String]): StringRDDFunctins = {
    new StringRDDFunctins(rdd)
  }

  implicit def dStreamToArrayByteDStreamFunctions(
      dstream: DStream[Array[Byte]]): ArrayByteDStreamFunctins = {
    new ArrayByteDStreamFunctins(dstream)
  }

  implicit def dStreamToStringDStreamFunctions(
      dstream: DStream[String]): StringDStreamFunctins = {
    new StringDStreamFunctins(dstream)
  }
}
