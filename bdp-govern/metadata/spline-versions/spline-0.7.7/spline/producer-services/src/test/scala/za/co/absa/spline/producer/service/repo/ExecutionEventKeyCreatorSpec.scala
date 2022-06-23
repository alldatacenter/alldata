/*
 * Copyright 2019 ABSA Group Limited
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

package za.co.absa.spline.producer.service.repo

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.spline.producer.model.v1_1.ExecutionEvent
import za.co.absa.spline.producer.service.model.ExecutionEventKeyCreator

import java.util.UUID

class ExecutionEventKeyCreatorSpec extends AnyFlatSpec with Matchers {

  "asExecutionEventKey" should "create an event key based on the execution plan ID and the event timestamp" in {
    val testEvent = ExecutionEvent(
      planId = UUID.fromString("00000000-0000-0000-0000-000000000000"),
      timestamp = 1234567890,
      durationNs = None,
      error = None,
      extra = Map.empty)

    new ExecutionEventKeyCreator(testEvent).executionEventKey should be("00000000-0000-0000-0000-000000000000:kf12oi")
  }

}
