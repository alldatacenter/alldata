/*
 * Copyright 2017 ABSA Group Limited
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

import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.{HandlerMethodReturnValueHandler, ModelAndViewContainer}

import scala.runtime.BoxedUnit

object UnitMethodReturnValueHandler {
  private val unitClasses = Seq(
    classOf[Unit],
    classOf[BoxedUnit])
}

/**
  * To be used in combination with @Controller method without @ResponseBody
  */
class UnitMethodReturnValueHandler extends HandlerMethodReturnValueHandler {

  import UnitMethodReturnValueHandler.unitClasses

  override def supportsReturnType(returnType: MethodParameter): Boolean =
    unitClasses contains returnType.getGenericParameterType

  override def handleReturnValue(rv: scala.Any, rt: MethodParameter, mavContainer: ModelAndViewContainer, wr: NativeWebRequest): Unit =
    mavContainer.setRequestHandled(true)
}