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

import java.util.UUID

import org.apache.commons.configuration.BaseConfiguration
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.scalatest.ConsoleStubs
import za.co.absa.spline.producer.model.ExecutionEvent

class ConsoleLineageDispatcherSpec
  extends AnyFlatSpec
    with Matchers
    with ConsoleStubs {

  behavior of "ConsoleLineageDispatcher"

  private val uuid1 = UUID.fromString("12345678-90ab-cdef-1234-567890abcdef")

  it should "send lineage to the stdout" in {
    assertingStdOut(include("""["event",{"planId":"12345678-90ab-cdef-1234-567890abcdef","timestamp":999}]""")) {
      new ConsoleLineageDispatcher(new BaseConfiguration {
        addProperty("stream", "OUT")
      }).send(ExecutionEvent(uuid1, None, 999, None, None, None, None))
    }
  }

  it should "send lineage to the stderr" in {
    assertingStdErr(include("""["event",{"planId":"12345678-90ab-cdef-1234-567890abcdef","timestamp":999}]""")) {
      new ConsoleLineageDispatcher(new BaseConfiguration {
        addProperty("stream", "ERR")
      }).send(ExecutionEvent(uuid1, None, 999, None, None, None, None))
    }
  }

}
