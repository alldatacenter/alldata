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

package za.co.absa.commons

import za.co.absa.commons.EnumUtils.EnumOps._

import scala.language.reflectiveCalls
import scala.reflect.ClassTag

object EnumUtils {

  trait EnumOps[A] {
    self: {def values: Seq[A]} =>

    object Implicits {
      implicit class ValueOps(v: A) {
        def name: String = getEnumName(v)
      }
    }

    def getEnumName(v: Any): String = {
      val EnumSimpleNameRegexp(name) = v.getClass.getName
      name
    }

    def valueOf(name: String)(implicit ct: ClassTag[A]): A = values
      .find(v => getEnumName(v).equalsIgnoreCase(name))
      .getOrElse {
        import Implicits._
        val enumTypeName = ct.runtimeClass.getSimpleName
        val availableNames = values.map(_.name)
        throw new IllegalArgumentException(s"String '$name' does not match any of the $enumTypeName enum values (${availableNames.mkString(", ")})")
      }
  }

  object EnumOps {
    private val EnumSimpleNameRegexp = """^(?:[^.]+\.)*(?:[^$]*\$)*([^$]+)\$*$""".r
  }

}
