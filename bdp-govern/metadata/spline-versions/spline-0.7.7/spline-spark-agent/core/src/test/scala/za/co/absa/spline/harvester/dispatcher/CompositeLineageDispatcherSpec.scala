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

import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.spline.producer.model.{ExecutionEvent, ExecutionPlan}

class CompositeLineageDispatcherSpec
  extends AnyFlatSpec
    with MockitoSugar
    with Matchers {

  it should "delegate calls" in {
    val dummyExecPlan = mock[ExecutionPlan]
    val dummyExecEvent = mock[ExecutionEvent]
    val mockDispatcher1 = mock[LineageDispatcher]
    val mockDispatcher2 = mock[LineageDispatcher]

    val compositeDispatcher = new CompositeLineageDispatcher(Seq(mockDispatcher1, mockDispatcher2), failOnErrors = true)
    compositeDispatcher.send(dummyExecPlan)
    compositeDispatcher.send(dummyExecEvent)

    verify(mockDispatcher1).send(dummyExecPlan)
    verify(mockDispatcher2).send(dummyExecPlan)

    verify(mockDispatcher1).send(dummyExecEvent)
    verify(mockDispatcher2).send(dummyExecEvent)

    verifyNoMoreInteractions(mockDispatcher1)
    verifyNoMoreInteractions(mockDispatcher2)
  }

  it should "fail on errors" in {
    val dummyExecPlan = mock[ExecutionPlan]
    val dummyExecEvent = mock[ExecutionEvent]
    val mockDispatcher1 = mock[LineageDispatcher]
    val mockDispatcher2 = mock[LineageDispatcher]

    when(mockDispatcher1.send(dummyExecPlan)) thenThrow new RuntimeException("boom")
    when(mockDispatcher1.send(dummyExecEvent)) thenThrow new RuntimeException("bam")

    val compositeDispatcher = new CompositeLineageDispatcher(Seq(mockDispatcher1, mockDispatcher2), failOnErrors = true)
    the[Exception] thrownBy compositeDispatcher.send(dummyExecPlan) should have message "boom"
    the[Exception] thrownBy compositeDispatcher.send(dummyExecEvent) should have message "bam"

    verifyNoMoreInteractions(mockDispatcher2)
  }

  it should "suppress errors" in {
    val dummyExecPlan = mock[ExecutionPlan]
    val dummyExecEvent = mock[ExecutionEvent]
    val mockDispatcher1 = mock[LineageDispatcher]
    val mockDispatcher2 = mock[LineageDispatcher]

    when(mockDispatcher1.send(dummyExecPlan)) thenThrow new RuntimeException("boom")
    when(mockDispatcher1.send(dummyExecEvent)) thenThrow new RuntimeException("bam")

    val compositeDispatcher = new CompositeLineageDispatcher(Seq(mockDispatcher1, mockDispatcher2), failOnErrors = false)
    compositeDispatcher.send(dummyExecPlan)
    compositeDispatcher.send(dummyExecEvent)

    verify(mockDispatcher2).send(dummyExecPlan)
    verify(mockDispatcher2).send(dummyExecEvent)

    verifyNoMoreInteractions(mockDispatcher2)
  }

  it should "not break on empty delegatees" in {
    val compositeDispatcher = new CompositeLineageDispatcher(Seq.empty, failOnErrors = true)

    compositeDispatcher.send(mock[ExecutionPlan])
    compositeDispatcher.send(mock[ExecutionEvent])

    succeed
  }
}
