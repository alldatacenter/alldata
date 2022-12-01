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
import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.commons.version.Version._
import za.co.absa.spline.harvester.dispatcher.SplineEntityType.{ExecutionEvent, ExecutionPlan}

class SplineRecordSenderFactorySpec
  extends AnyFlatSpec
    with MockitoSugar {

  it should "manage producer lifecycle" in {
    val mockProducer = mock[Producer[String, String]]
    val mockProducerCreator = mock[() => Producer[String, String]]
    when(mockProducerCreator()) thenReturn mockProducer
    val factory = new SplineRecordSenderFactory(ver"42", "dummyTopic", mockProducerCreator)

    factory.createSender(ExecutionPlan)
    factory.createSender(ExecutionEvent)

    verify(mockProducerCreator, only)()
    verify(mockProducer, never).close()

    factory.close()

    verify(mockProducer, only).close()

    verifyNoMoreInteractions(mockProducerCreator)
    verifyNoMoreInteractions(mockProducer)
  }
}
