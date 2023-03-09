/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.griffin.measure.step

import scala.util.Try

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._

import org.apache.griffin.measure.{Loggable, SparkSuiteBase}
import org.apache.griffin.measure.configuration.enums.ProcessType.BatchProcessType
import org.apache.griffin.measure.context.{ContextId, DQContext}
import org.apache.griffin.measure.step.transform.TransformStep

class TransformStepTest extends AnyFlatSpec with Matchers with SparkSuiteBase with Loggable {

  case class DualTransformStep(
      name: String,
      duration: Int,
      rule: String = "",
      details: Map[String, Any] = Map(),
      cache: Boolean = false)
      extends TransformStep {

    def doExecute(context: DQContext): Try[Boolean] = Try {
      val threadName = Thread.currentThread().getName
      info(s"Step $name started with $threadName")
      Thread.sleep(duration * 1000L)
      info(s"Step $name finished with $threadName")
      true
    }
  }

  private def getDqContext(name: String = "test-context"): DQContext = {
    DQContext(ContextId(System.currentTimeMillis), name, Nil, Nil, BatchProcessType)(spark)
  }

  /**
   * Run transform steps in parallel. Here are the dependencies of transform steps
   *
   * step5
   * |   |---step2
   * |   |   |---step1
   * |   |---step3
   * |   |   |---step1
   * |   |---step4
   *
   * step1 : -->
   * step2 :    --->
   * step3 :    ---->
   * step4 : ->
   * step5 :         -->
   *
   */
  "transform step " should "be run steps in parallel" in {
    val step1 = DualTransformStep("step1", 3)
    val step2 = DualTransformStep("step2", 4)
    step2.parentSteps += step1
    val step3 = DualTransformStep("step3", 5)
    step3.parentSteps += step1
    val step4 = DualTransformStep("step4", 2)
    val step5 = DualTransformStep("step5", 3)
    step5.parentSteps += step2
    step5.parentSteps += step3
    step5.parentSteps += step4

    val context = getDqContext()
    step5.execute(context).get should be(true)
  }
}
