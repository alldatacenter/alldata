/*
 * Copyright 2020 ABSA Group Limited
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

package za.co.absa.spline.harvester.logging

import za.co.absa.commons.ThrowableImplicits._
import za.co.absa.commons.reflect.ReflectionUtils

import java.lang.reflect.{Field, Modifier}
import scala.annotation.tailrec
import scala.util.Random
import scala.util.control.NonFatal

object ObjectStructureDumper {

  type FieldName = String
  type FieldType = String
  type DumpResult = String

  type DumpFn = Any => DumpResult
  type ExtractFieldValueFn = (AnyRef, FieldName) => AnyRef

  def dump(obj: Any, extractFieldValueFn: ExtractFieldValueFn = ReflectionUtils.extractFieldValue[AnyRef]): DumpResult = {
    val value = obj.asInstanceOf[AnyRef]

    val initialValue = ObjectBox(value, "", value.getClass.getName, 0)

    val info = objectToStringRec(extractFieldValueFn)(List(initialValue), Set.empty[InstanceEqualityBox], "")
    val filler = "*" * 30
    s"""
       |$filler OBJECT DUMP BEGIN $filler
       |${value.getClass}
       |$info
       |$filler OBJECT DUMP END   $filler
       |""".stripMargin
  }

  private case class InstanceEqualityBox(obj: AnyRef) {
    override def equals(otherObj: Any): Boolean = otherObj match {
      case InstanceEqualityBox(inner) => inner eq obj
      case _ => throw new UnsupportedOperationException()
    }

    override def hashCode(): Int = obj.getClass.getName.hashCode
  }

  private type VisitedSet = Set[InstanceEqualityBox]

  private def addToVisited(visited: VisitedSet, obj: AnyRef): VisitedSet = visited + InstanceEqualityBox(obj)

  private def wasVisited(visited: VisitedSet, obj: AnyRef): Boolean = visited(InstanceEqualityBox(obj))

  private case class ObjectBox(value: AnyRef, fieldName: FieldName, fieldType: FieldType, depth: Int)

  @tailrec
  private final def objectToStringRec(
    extractFieldValue: ExtractFieldValueFn
  )(
    stack: List[ObjectBox],
    visited: VisitedSet,
    prevResult: DumpResult
  ): DumpResult = stack match {
    case Nil => prevResult
    case head :: tail => {
      val value = head.value
      val depth = head.depth

      val (fieldsDetails, newStack, newVisited) = value match {
        case null => ("= null", tail, visited)
        case v if isReadyForPrint(v) => (s"= $v", tail, visited)
        case v if wasVisited(visited, v) => ("! Object was already logged", tail, visited)
        case None => ("= None", tail, visited)
        case Some(x) => {
          val newVal = ObjectBox(x.asInstanceOf[AnyRef], "x", x.getClass.getName, depth + 1)
          ("Some", newVal :: tail, addToVisited(visited, value))
        }
        case _ => {
          val newFields = value.getClass.getDeclaredFields
            .filter(!isIgnoredField(_))
            .map { f =>
              val subValue =
                try {
                  extractFieldValue(value, f.getName)
                } catch {
                  case e @ (_:LinkageError | NonFatal(_)) => s"! error occurred: ${e.toShortString}"
                }
              ObjectBox(subValue, f.getName, f.getType.getName, depth + 1)
            }.toList

          ("", newFields ::: tail, addToVisited(visited, value))
        }
      }

      val indent = " " * depth * 2

      val line =
        if (depth > 0) s"$indent${head.fieldName}: ${head.fieldType} $fieldsDetails"
        else prevResult

      val newResult =
        if (prevResult.isEmpty) line
        else s"$prevResult\n$line"

      objectToStringRec(extractFieldValue)(newStack, newVisited, newResult)
    }
  }
  private def isIgnoredField(f: Field): Boolean = {
    Set("child", "session")(f.getName) ||
      Modifier.isStatic(f.getModifiers) ||
      Modifier.isTransient(f.getModifiers)
  }

  private def isReadyForPrint(value: AnyRef): Boolean = {
    isPrimitiveLike(value) ||
      value.isInstanceOf[java.lang.CharSequence]  ||
      value.isInstanceOf[Traversable[_]] ||
      value.isInstanceOf[Enum[_]] ||
      value.isInstanceOf[java.util.Random] ||
      value.isInstanceOf[Random] ||
      value.isInstanceOf[java.lang.Number] ||
      value.isInstanceOf[Numeric[_]] ||
      value.isInstanceOf[Class[_]]
  }

  private def isPrimitiveLike(value: Any): Boolean = {
    val boxes = Set(
      classOf[java.lang.Boolean],
      classOf[java.lang.Byte],
      classOf[java.lang.Character],
      classOf[java.lang.Float],
      classOf[java.lang.Integer],
      classOf[java.lang.Long]
    ).map(_.getName)

    val isPrimitive = (value: Any) => value.getClass.isPrimitive
    val isBoxedPrimitive = (value: Any) => boxes(value.getClass.getName)

    isPrimitive(value) || isBoxedPrimitive(value)
  }
}
