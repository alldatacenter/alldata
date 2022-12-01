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

package za.co.absa.spline.harvester.postprocessing

import org.mockito.Mockito
import org.mockito.Mockito.{verifyNoMoreInteractions, when}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.spline.harvester.HarvestingContext
import za.co.absa.spline.producer.model._

class CompositePostProcessingFilterSpec
  extends AnyFlatSpec
    with Matchers
    with MockitoSugar {

  it should "chain calls" in {
    val dummyCtx = mock[HarvestingContext]

    val dummyPlan1 = mock[ExecutionPlan]
    val dummyPlan2 = mock[ExecutionPlan]
    val dummyPlan3 = mock[ExecutionPlan]

    val dummyEvent1 = mock[ExecutionEvent]
    val dummyEvent2 = mock[ExecutionEvent]
    val dummyEvent3 = mock[ExecutionEvent]

    val dummyReadOp1 = mock[ReadOperation]
    val dummyReadOp2 = mock[ReadOperation]
    val dummyReadOp3 = mock[ReadOperation]

    val dummyWriteOp1 = mock[WriteOperation]
    val dummyWriteOp2 = mock[WriteOperation]
    val dummyWriteOp3 = mock[WriteOperation]

    val dummyDataOp1 = mock[DataOperation]
    val dummyDataOp2 = mock[DataOperation]
    val dummyDataOp3 = mock[DataOperation]

    val mockFilter1 = mock[PostProcessingFilter]
    when(mockFilter1.processExecutionPlan(dummyPlan1, dummyCtx)) thenReturn dummyPlan2
    when(mockFilter1.processExecutionEvent(dummyEvent1, dummyCtx)) thenReturn dummyEvent2
    when(mockFilter1.processReadOperation(dummyReadOp1, dummyCtx)) thenReturn dummyReadOp2
    when(mockFilter1.processWriteOperation(dummyWriteOp1, dummyCtx)) thenReturn dummyWriteOp2
    when(mockFilter1.processDataOperation(dummyDataOp1, dummyCtx)) thenReturn dummyDataOp2

    val mockFilter2 = mock[PostProcessingFilter]
    when(mockFilter2.processExecutionPlan(dummyPlan2, dummyCtx)) thenReturn dummyPlan3
    when(mockFilter2.processExecutionEvent(dummyEvent2, dummyCtx)) thenReturn dummyEvent3
    when(mockFilter2.processReadOperation(dummyReadOp2, dummyCtx)) thenReturn dummyReadOp3
    when(mockFilter2.processWriteOperation(dummyWriteOp2, dummyCtx)) thenReturn dummyWriteOp3
    when(mockFilter2.processDataOperation(dummyDataOp2, dummyCtx)) thenReturn dummyDataOp3

    val compositeDispatcher = new CompositePostProcessingFilter(Seq(mockFilter1, mockFilter2))

    compositeDispatcher.processExecutionPlan(dummyPlan1, dummyCtx) should be theSameInstanceAs dummyPlan3
    compositeDispatcher.processExecutionEvent(dummyEvent1, dummyCtx) should be theSameInstanceAs dummyEvent3
    compositeDispatcher.processReadOperation(dummyReadOp1, dummyCtx) should be theSameInstanceAs dummyReadOp3
    compositeDispatcher.processWriteOperation(dummyWriteOp1, dummyCtx) should be theSameInstanceAs dummyWriteOp3
    compositeDispatcher.processDataOperation(dummyDataOp1, dummyCtx) should be theSameInstanceAs dummyDataOp3

    val inOrder = Mockito.inOrder(mockFilter1, mockFilter2)

    inOrder.verify(mockFilter1).processExecutionPlan(dummyPlan1, dummyCtx)
    inOrder.verify(mockFilter2).processExecutionPlan(dummyPlan2, dummyCtx)

    inOrder.verify(mockFilter1).processExecutionEvent(dummyEvent1, dummyCtx)
    inOrder.verify(mockFilter2).processExecutionEvent(dummyEvent2, dummyCtx)

    inOrder.verify(mockFilter1).processReadOperation(dummyReadOp1, dummyCtx)
    inOrder.verify(mockFilter2).processReadOperation(dummyReadOp2, dummyCtx)

    inOrder.verify(mockFilter1).processWriteOperation(dummyWriteOp1, dummyCtx)
    inOrder.verify(mockFilter2).processWriteOperation(dummyWriteOp2, dummyCtx)

    inOrder.verify(mockFilter1).processDataOperation(dummyDataOp1, dummyCtx)
    inOrder.verify(mockFilter2).processDataOperation(dummyDataOp2, dummyCtx)

    verifyNoMoreInteractions(mockFilter1)
    verifyNoMoreInteractions(mockFilter2)
  }
}
