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

package za.co.absa.spline.harvester.json

import org.json4s.JValue
import org.json4s.jackson.JsonMethods
import za.co.absa.commons.json.AbstractJsonSerDe
import za.co.absa.commons.json.format.JavaTypesSupport
import za.co.absa.commons.reflect.ReflectionUtils

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

object HarvesterJsonSerDe {

  private def classNameOf[A: ClassTag] = {
    implicitly[ClassTag[A]].runtimeClass.getCanonicalName
  }

  private val JsonMethods = classNameOf[JsonMethods]
  private val JavaTypesSupport = classNameOf[JavaTypesSupport]
  private val ShortTypeHintForSpline03ModelSupport = classNameOf[ShortTypeHintForSpline03ModelSupport]
  private val JValue = classNameOf[JValue]
  private val AbstractJsonSerDe = classNameOf[AbstractJsonSerDe[_]]

  // This delays the compilation (to bytecode) of that piece of code at runtime.
  // Commons are build against json4s 3.5.5, spark 2.4 usually provides json4s 3.5.3 and these are not binary compatible!
  val impl: AbstractJsonSerDe[JValue] = {
    ReflectionUtils.compile(runtimeMirror(getClass.getClassLoader).mkToolBox().parse(
      s"""
        new $AbstractJsonSerDe[$JValue]
          with $JsonMethods
          with $ShortTypeHintForSpline03ModelSupport
          with $JavaTypesSupport
      """))(Map.empty)
  }
}
