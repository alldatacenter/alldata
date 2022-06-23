/*
 * Copyright 2019 ABSA Group Limited
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

package za.co.absa.spline.test.fixture.spline

import za.co.absa.spline.harvester.dispatcher.LineageDispatcher
import za.co.absa.spline.producer.model.{ExecutionEvent, ExecutionPlan}

class LineageCapturingDispatcher(lineageCaptor: LineageCaptor.Setter) extends LineageDispatcher {

  override def name = "Test"

  override def send(plan: ExecutionPlan): Unit = {
    lineageCaptor.capture(plan)
  }

  override def send(event: ExecutionEvent): Unit = {
    lineageCaptor.capture(event)
  }
}
