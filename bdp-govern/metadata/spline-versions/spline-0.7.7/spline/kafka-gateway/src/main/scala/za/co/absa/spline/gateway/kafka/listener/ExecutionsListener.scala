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

package za.co.absa.spline.gateway.kafka.listener

import org.slf4s.Logging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.{KafkaHandler, KafkaListener}
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import za.co.absa.commons.annotation.Unstable
import za.co.absa.spline.gateway.kafka.KafkaGatewayConfig
import za.co.absa.spline.producer.model.v1_1.{ExecutionEvent, ExecutionPlan}
import za.co.absa.spline.producer.service.repo.ExecutionProducerRepository

import scala.beans.BeanProperty
import scala.concurrent.{Await, ExecutionContext}
import scala.util.control.NonFatal

@Unstable
@Component
@KafkaListener(topics = Array("#{__listener.topic}"))
class ExecutionsListener @Autowired()(val repo: ExecutionProducerRepository) extends Logging {

  import ExecutionContext.Implicits.global

  @BeanProperty
  val topic: String = KafkaGatewayConfig.Kafka.Topic

  @KafkaHandler
  def listenExecutionPlan(
    plan: ExecutionPlan,
    @Header(KafkaHeaders.OFFSET) offset: Long,
    @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
  ): Unit = {
    try {
      Await.result(repo.insertExecutionPlan(plan), KafkaGatewayConfig.Kafka.PlanTimeout)
    } catch {
      case NonFatal(e) => log.error(
        s"Error while inserting execution plan id:${plan.id} from topic:$topic on offset:$offset", e)
    }
  }

  @KafkaHandler
  def listenExecutionEvent(
    event: ExecutionEvent,
    @Header(KafkaHeaders.OFFSET) offset: Long,
    @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
  ): Unit = {
    try {
      Await.result(repo.insertExecutionEvents(Array(event)), KafkaGatewayConfig.Kafka.EventTimeout)
    } catch {
      case NonFatal(e) => log.error(
        s"Error while inserting execution event for planId:${event.planId} and timestamp: ${event.timestamp} " +
          s"from topic:$topic on offset:$offset", e)
    }
  }
}
