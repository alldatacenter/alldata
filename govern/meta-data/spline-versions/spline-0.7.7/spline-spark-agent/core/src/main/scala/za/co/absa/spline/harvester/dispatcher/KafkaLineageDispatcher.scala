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

package za.co.absa.spline.harvester.dispatcher

import org.apache.commons.configuration.{Configuration, ConfigurationConverter}
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.spark.internal.Logging
import org.apache.spark.sql.SparkSession
import za.co.absa.commons.config.ConfigurationImplicits.ConfigurationRequiredWrapper
import za.co.absa.commons.version.Version
import za.co.absa.spline.harvester.dispatcher.KafkaLineageDispatcher._
import za.co.absa.spline.harvester.dispatcher.kafkadispatcher._
import za.co.absa.spline.harvester.dispatcher.modelmapper.ModelMapper
import za.co.absa.spline.producer.model.{ExecutionEvent, ExecutionPlan}

/**
 * KafkaLineageDispatcher is responsible for sending the lineage data to spline gateway through kafka
 */
class KafkaLineageDispatcher(
  apiVersion: Version,
  createSenderFactory: Version => SplineRecordSenderFactory
) extends LineageDispatcher
  with Logging {

  import za.co.absa.spline.harvester.json.HarvesterJsonSerDe.impl._

  def this(configuration: Configuration, sparkSession: SparkSession) = this(
    Version.asSimple(configuration.getRequiredString(ApiVersion)),
    apiVersion => new SplineRecordSenderFactory(
      apiVersion,
      configuration.getRequiredString(TopicKey),
      () => {
        val kp = new KafkaProducer[String, String](ConfigurationConverter.getProperties(configuration.subset(ProducerKey)))
        sparkSession.sparkContext.addSparkListener(new AppEndListener(kp.close))
        kp
      }
    )
  )

  override def name = "Kafka"

  private val senderFactory = {
    val sf = createSenderFactory(apiVersion)
    sys.addShutdownHook(sf.close())
    sf
  }

  private val planRecordSender = senderFactory.createSender[ExecutionPlan](SplineEntityType.ExecutionPlan)
  private val eventRecordSender = senderFactory.createSender[ExecutionEvent](SplineEntityType.ExecutionEvent)
  private val modelMapper = ModelMapper.forApiVersion(apiVersion)

  override def send(plan: ExecutionPlan): Unit = {
    for (planDTO <- modelMapper.toDTO(plan)) {
      planRecordSender.send(planDTO.toJson, plan.id.get)
    }
  }

  override def send(event: ExecutionEvent): Unit = {
    for (eventDTO <- modelMapper.toDTO(event)) {
      eventRecordSender.send(eventDTO.toJson, event.planId)
    }
  }

}

object KafkaLineageDispatcher {
  private val TopicKey = "topic"
  private val ApiVersion = "apiVersion"
  private val ProducerKey = "producer"
}
