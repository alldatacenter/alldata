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

package za.co.absa.spline.common.webmvc

import java.util.concurrent.CompletionStage

import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.springframework.core.MethodParameter
import org.springframework.util.concurrent.ListenableFuture
import org.springframework.web.context.request.async.DeferredResult
import za.co.absa.spline.common.webmvc.ScalaFutureMethodReturnValueHandlerSpec.{SubFuture, mockWith}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class ScalaFutureMethodReturnValueHandlerSpec extends AnyFlatSpec with MockitoSugar with Matchers {

  private implicit val ec: ExecutionContext = mock[ExecutionContext]
  private val handler = new ScalaFutureMethodReturnValueHandler

  behavior of "ScalaFutureMethodReturnValueHandler"

  it should "supportsReturnType" in {
    handler.supportsReturnType(mockWith[MethodParameter, Class[_]](_.getParameterType, classOf[Future[_]])) should be(true)
    handler.supportsReturnType(mockWith[MethodParameter, Class[_]](_.getParameterType, classOf[SubFuture])) should be(true)
    handler.supportsReturnType(mockWith[MethodParameter, Class[_]](_.getParameterType, classOf[DeferredResult[_]])) should be(true)
    handler.supportsReturnType(mockWith[MethodParameter, Class[_]](_.getParameterType, classOf[ListenableFuture[_]])) should be(true)
    handler.supportsReturnType(mockWith[MethodParameter, Class[_]](_.getParameterType, classOf[CompletionStage[_]])) should be(true)
    handler.supportsReturnType(mockWith[MethodParameter, Class[_]](_.getParameterType, classOf[AnyRef])) should be(false)
  }

  "isAsyncReturnValue" should "return true for any Future type" in {
    handler.isAsyncReturnValue(mock[Future[_]], null) should be(true)
    handler.isAsyncReturnValue(mock[SubFuture], null) should be(true)
    handler.isAsyncReturnValue(mock[AnyRef], null) should be(false)
    handler.isAsyncReturnValue(null, null) should be(false)
  }

}

object ScalaFutureMethodReturnValueHandlerSpec extends MockitoSugar {

  abstract class SubFuture extends Future[Any]

  private def mockWith[A <: AnyRef : ClassTag, B](call: A => B, retVal: B): A = {
    val aMock = mock[A]
    when(call(aMock)).thenReturn(retVal)
    aMock
  }
}
