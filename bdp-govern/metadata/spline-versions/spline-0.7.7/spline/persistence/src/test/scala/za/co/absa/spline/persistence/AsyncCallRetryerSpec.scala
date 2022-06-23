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

package za.co.absa.spline.persistence

import org.mockito.Mockito._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.spline.common.AsyncCallRetryer

import scala.concurrent.Future
import scala.concurrent.Future._
import scala.language.implicitConversions

class AsyncCallRetryerSpec
  extends AsyncFlatSpec
    with Matchers
    with MockitoSugar {

  behavior of "isRetryable()"

  it should "call an underlying method and return a result" in {
    val retryer = new AsyncCallRetryer(mock[Throwable => Boolean], 5)
    val spy = mock[() => Future[String]]
    when(spy()) thenReturn successful("result")
    for (result <- retryer.execute(spy()))
      yield {
        verify(spy, times(1))()
        result should equal("result")
      }
  }

  it should "retry after a recoverable failure" in {
    val retryer = new AsyncCallRetryer(_ => true, 5)
    val spy = mock[() => Future[String]]
    (when(spy())
      thenReturn failed(new Exception("1st call failed"))
      thenReturn failed(new Exception("2nd call failed"))
      thenReturn successful("3rd call succeeded"))
    for (result <- retryer.execute(spy()))
      yield {
        verify(spy, times(3))()
        result should equal("3rd call succeeded")
      }
  }

  it should "only retry up to the maximum number of retries" in {
    val retryer = new AsyncCallRetryer(_ => true, 2)
    val spy = mock[() => Future[String]]
    when(spy()) thenReturn failed(new Exception("oops"))
    for (thrown <- recoverToExceptionIf[Exception](retryer.execute(spy())))
      yield {
        verify(spy, times(3))() // 2 retries + 1 initial call
        thrown.getMessage should equal("oops")
      }
  }

  it should "not retry on a non-recoverable error" in {
    val retryer = new AsyncCallRetryer(_ => false, 5)
    val spy = mock[() => Future[String]]
    when(spy()) thenReturn failed(new RuntimeException("boom"))
    for (thrown <- recoverToExceptionIf[Exception](retryer.execute(spy())))
      yield {
        verify(spy, times(1))()
        thrown.getMessage should equal("boom")
      }
  }
}
