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

import org.apache.commons.lang3.StringUtils._
import org.json4s.{DefaultFormats, Formats, TypeHints}
import za.co.absa.commons.json.format.FormatsBuilder
import za.co.absa.commons.reflect.ReflectionUtils._
import za.co.absa.spline.harvester.json.ShortTypeHintForSpline03ModelSupport._
import za.co.absa.spline.model

import scala.reflect.runtime.universe._

trait ShortTypeHintForSpline03ModelSupport extends FormatsBuilder {
  override protected def formats: Formats = createFormats(Map(
    "typeHintFieldName" -> "_typeHint",
    "typeHintClasses" -> directSubClassesOf[model.dt.DataType],
    "dateFormatterFn" -> DefaultFormats.losslessDate.get _))
}

object ShortTypeHintForSpline03ModelSupport {

  private val createFormats =
    if (isJson4sVerAtLeast37) createFormats37
    else createFormats32

  private def isJson4sVerAtLeast37: Boolean =
    classOf[TypeHints].getMethods.exists(_.getName == "typeHintFieldName")

  abstract class AbstractSplineShortTypeHints(classes: Seq[Class[_]]) extends TypeHints {
    override val hints: List[Class[_]] = classes.toList

    def classFor(hint: String, a: Class[_]): Option[Class[_]] = classFor(hint)

    def classFor(hint: String): Option[Class[_]] = classes find (hintForAsString(_) == hint)

    // TypeHints.hintFor() has a different return type in Json4s >= 3.7
    // So we should not use it directly, and use this method instead.
    protected def hintForAsString(clazz: Class[_]): String = {
      val className = clazz.getName
      className.substring(1 + lastOrdinalIndexOf(className, ".", 2))
    }
  }

  private def createFormats32: Map[String, Any] => Formats = compile[Formats](
    q"""
      import java.text.SimpleDateFormat
      import org.json4s.{DefaultFormats, TypeHints}
      import za.co.absa.spline.harvester.json.ShortTypeHintForSpline03ModelSupport.AbstractSplineShortTypeHints

      new DefaultFormats {
        override def dateFormatter = args[() => SimpleDateFormat]("dateFormatterFn")()
        override val typeHintFieldName: String = args("typeHintFieldName")
        override val typeHints: TypeHints = {
          new AbstractSplineShortTypeHints(args("typeHintClasses")) {
            override def hintFor(clazz: Class[_]): String = super.hintForAsString(clazz)
          }
        }
      }
    """)

  private def createFormats37: Map[String, Any] => Formats = compile[Formats](
    q"""
      import java.text.SimpleDateFormat
      import org.json4s.{DefaultFormats, TypeHints}
      import za.co.absa.spline.harvester.json.ShortTypeHintForSpline03ModelSupport.AbstractSplineShortTypeHints

      new DefaultFormats {
        override def dateFormatter = args[() => SimpleDateFormat]("dateFormatterFn")()
        override val typeHints: TypeHints = {
          new AbstractSplineShortTypeHints(args("typeHintClasses")) {
            override def hintFor(clazz: Class[_]): Option[String] = Option(super.hintForAsString(clazz))
            override val typeHintFieldName: String = args("typeHintFieldName")
          }
        }
      }
    """)
}
