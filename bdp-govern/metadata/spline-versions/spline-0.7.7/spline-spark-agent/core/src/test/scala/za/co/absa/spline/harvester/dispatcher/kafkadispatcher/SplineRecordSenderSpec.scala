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

import org.apache.kafka.clients.producer.{Producer, ProducerRecord, RecordMetadata}
import org.mockito.{ArgumentCaptor, ArgumentMatchers, Mockito}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.commons.version.Version._
import za.co.absa.spline.harvester.dispatcher.SplineEntityType.ExecutionEvent
import za.co.absa.spline.harvester.dispatcher.kafkadispatcher.SplineRecordSenderSpec.Foo

import java.util.UUID
import java.util.concurrent.CompletableFuture.completedFuture

class SplineRecordSenderSpec
  extends AnyFlatSpec
    with MockitoSugar
    with Matchers {

  "send()" should "send a record to producer" in {
    val dummyContent = "dummy content"
    val dummyId = UUID.randomUUID()
    val dummyVersion = ver"42"
    val dummyTopic = "dummyTopic"

    val c: ArgumentCaptor[ProducerRecord[String, String]] = ArgumentCaptor.forClass(classOf[ProducerRecord[String, String]])

    val mockProducer = mock[Producer[String, String]]
    Mockito.when(mockProducer.send(c.capture())).thenReturn(completedFuture[RecordMetadata](null))

    val sender = SplineRecordSender[Foo](ExecutionEvent, dummyVersion, dummyTopic, mockProducer)

    Mockito.verifyNoInteractions(mockProducer)

    sender.send(dummyContent, dummyId)
    val captiredRecord = c.getValue

    Mockito.verify(mockProducer, Mockito.only).send(ArgumentMatchers.any())
    Mockito.verifyNoMoreInteractions(mockProducer)

    captiredRecord.partition() shouldBe null
    captiredRecord.key() shouldEqual dummyId.toString
    captiredRecord.value() should be theSameInstanceAs dummyContent
    captiredRecord.topic() should be theSameInstanceAs dummyTopic

    val headers = captiredRecord.headers().toArray.map(h => h.key -> new String(h.value))
    headers should have length 3
    headers should contain theSameElementsAs Seq(
      "ABSA-Spline-API-Version" -> "42",
      "ABSA-Spline-Entity-Type" -> "ExecutionEvent",
      "__TypeId__" -> "ExecutionEvent_42"
    )
  }
}

object SplineRecordSenderSpec {
  case class Foo(x: Int)
}
