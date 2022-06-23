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

package za.co.absa.spline.harvester.converter

import org.apache.spark.internal.Logging
import za.co.absa.commons.reflect.ReflectionUtils

import java.lang.reflect.{Field, Method}
import scala.util.control.NonFatal

object ReflectiveExtractor extends Logging {

  def extractProperties(obj: AnyRef): Map[String, _] =
    try ReflectionUtils.extractProperties(obj)
    catch {
      // a workaround for Scala bug #12190
      // catching everything because the bug causes various errors and exceptions
      case NonFatal(e) =>
        logWarning("Extracting object properties via Scala reflection failed. Trying Java reflection as a workaround...", e)

        extractPropertiesViaJavaReflection(obj)
    }

  private def extractPropertiesViaJavaReflection(obj: AnyRef): Map[String, _] = {
    val methods = obj.getClass
      .getDeclaredMethods
      .filterNot(m => m.isSynthetic || m.getParameterCount > 0)

    def isCorrespondingMethod(f: Field, m: Method): Boolean =
      m.getName == f.getName && f.getType == m.getReturnType

    val fields = obj.getClass
      .getDeclaredFields
      .filterNot(_.isSynthetic)
      .filter(f => methods.exists(isCorrespondingMethod(f, _)))

    fields.map { f =>
      f.setAccessible(true)
      f.getName -> f.get(obj)
    }.toMap
  }

}
