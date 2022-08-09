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

import org.apache.kafka.clients.producer.Producer
import org.apache.spark.internal.Logging
import za.co.absa.commons.version.Version
import za.co.absa.spline.harvester.dispatcher.SplineEntityType

class SplineRecordSenderFactory(
  apiVersion: Version,
  topic: String,
  createProducer: () => Producer[String, String]
) extends Logging
  with AutoCloseable {

  logInfo(s"Kafka topic: $topic")
  logDebug(s"Producer API version: $apiVersion")

  private val producer: Producer[String, String] = createProducer()

  override def close(): Unit = {
    producer.close()
  }

  def createSender[A <: AnyRef](entityType: SplineEntityType): SplineRecordSender = {
    SplineRecordSender(entityType, apiVersion, topic, producer)
  }
}
