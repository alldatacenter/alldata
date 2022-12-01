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

import org.apache.commons.configuration.Configuration
import za.co.absa.commons.config.ConfigurationImplicits.ConfigurationRequiredWrapper

import scala.reflect.ClassTag
import scala.util.control.NonFatal

object ConfigurationImplicits {

  implicit class ConfOps(val conf: Configuration) extends AnyVal {

    def getRequiredEnum[A <: Enum[A] : ClassTag](key: String): A = {
      val clazz = implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]]
      val name = conf.getRequiredString(key)
      try {
        Enum.valueOf[A](clazz, name)
      } catch {
        case NonFatal(e) =>
          val values = clazz.getEnumConstants
          throw new IllegalArgumentException(s"Invalid value for property $key=$name. Should be one of: ${values mkString ", "}", e)
      }
    }
  }
}
