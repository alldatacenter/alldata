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

package za.co.absa.spline.common

import org.slf4s.Logging
import za.co.absa.spline.common.AsyncCallRetryer._

import scala.concurrent.{ExecutionContext, Future}

class AsyncCallRetryer(isRetryable: Throwable => Boolean, maxRetries: Int) extends Logging {

  def execute[R](fn: => Future[R])(implicit ex: ExecutionContext): Future[R] = {
    executeWithRetry(fn, None)
  }

  private def executeWithRetry[R](fn: => Future[R], lastFailure: Option[FailedAttempt])(implicit ex: ExecutionContext): Future[R] = {
    val eventualResult = fn
    val attemptsUsed = lastFailure.map(_.count).getOrElse(0)

    for (failure <- lastFailure) eventualResult.foreach { _ =>
      log.warn(s"Succeeded after ${failure.count + 1} attempts. Previous message was: ${failure.error.getMessage}")
    }

    if (attemptsUsed >= maxRetries)
      eventualResult
    else
      eventualResult.recoverWith {
        case e if isRetryable(e) => executeWithRetry(fn, Some(FailedAttempt(attemptsUsed + 1, e)))
      }
  }
}

object AsyncCallRetryer {
  case class FailedAttempt(count: Int, error: Throwable)
}
