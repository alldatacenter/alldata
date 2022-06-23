/*
 * Copyright 2021 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.harvester.dispatcher.kafkadispatcher

import org.apache.kafka.clients.producer.{Producer, RecordMetadata}
import org.apache.spark.internal.Logging
import za.co.absa.commons.version.Version
import za.co.absa.spline.harvester.dispatcher.SplineEntityType

import java.util.UUID
import scala.util.control.NonFatal

trait SplineRecordSender {
  def send(json: String, id: UUID): RecordMetadata
}

object SplineRecordSender extends Logging {

  def apply[A <: AnyRef](
    entityType: SplineEntityType,
    apiVersion: Version,
    topic: String,
    producer: Producer[String, String]
  ): SplineRecordSender = new SplineRecordSender {

    import za.co.absa.spline.harvester.json.HarvesterJsonSerDe.impl._

    private val headers = Array(
      new ApiVersionHeader(apiVersion),
      new EntityTypeHeader(entityType),
      new SpringClassIdHeader(entityType, apiVersion)
    )

    def send(json: String, id: UUID): RecordMetadata = {
      val record = new SplineProducerRecord(topic, id, json, headers)

      logTrace(
        s"""Sending message to kafka topic: ${record.topic}
           |Key: ${record.key}
           |Value: ${record.value.asPrettyJson}
           |""".stripMargin)

      try {
        producer.send(record).get()
      } catch {
        case NonFatal(e) =>
          throw new RuntimeException(s"Cannot send lineage data to kafka topic ${record.topic}", e)
      }
    }
  }

}
