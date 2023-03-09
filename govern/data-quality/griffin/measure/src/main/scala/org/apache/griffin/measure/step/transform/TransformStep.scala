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

package org.apache.griffin.measure.step.transform

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

import org.apache.griffin.measure.context.DQContext
import org.apache.griffin.measure.step.{DQStep, DQStepStatus}
import org.apache.griffin.measure.step.DQStepStatus._
import org.apache.griffin.measure.utils.ThreadUtils

trait TransformStep extends DQStep {

  val rule: String

  val details: Map[String, Any]

  val cache: Boolean

  var status: DQStepStatus.Value = PENDING

  val parentSteps = new mutable.HashSet[TransformStep]

  def doExecute(context: DQContext): Try[Boolean]

  def execute(context: DQContext): Try[Boolean] = {

    val threadName = Thread.currentThread().getName
    info(threadName + " begin transform step : \n" + debugString())

    // Submit parents Steps
    val parentStepFutures = parentSteps.filter(checkAndUpdateStatus).map { parentStep =>
      Future {
        val result = parentStep.execute(context)
        parentStep.synchronized {
          result match {
            case Success(_) => parentStep.status = COMPLETE
            case Failure(_) => parentStep.status = FAILED
          }
        }
        result
      }(TransformStep.transformStepContext)
    }

    val parentsResultSet = ThreadUtils.awaitResult(
      Future.sequence(parentStepFutures)(implicitly, TransformStep.transformStepContext),
      Duration.Inf)

    val parentsResult = parentsResultSet.foldLeft(Try(true)) { (ret, step) =>
      (ret, step) match {
        case (Success(_), nextResult) => nextResult
        case (Failure(_), _) => ret
      }
    }

    parentSteps.foreach(step => {
      while (step.status == RUNNING) {
        Thread.sleep(1000L)
      }
    })

    parentsResult match {
      case Success(_) =>
        info(threadName + " end transform step : \n" + debugString())
        doExecute(context)
      case Failure(e) =>
        error("Parent transform step failed: \n" + debugString(), e)
        parentsResult
    }
  }

  def checkAndUpdateStatus(step: TransformStep): Boolean = {
    step.synchronized {
      if (step.status == PENDING) {
        step.status = RUNNING
        true
      } else {
        false
      }
    }
  }

  def debugString(level: Int = 0): String = {
    val stringBuffer = new StringBuilder
    if (level > 0) {
      for (_ <- 0 until level) {
        stringBuffer.append("|   ")
      }
      stringBuffer.append("|---")
    }
    stringBuffer.append(name + "\n")
    parentSteps.foreach(parentStep => stringBuffer.append(parentStep.debugString(level + 1)))
    stringBuffer.toString()
  }
}

object TransformStep {
  private[transform] val transformStepContext =
    ExecutionContext.fromExecutorService(ThreadUtils.newDaemonCachedThreadPool("transform-step"))
}
