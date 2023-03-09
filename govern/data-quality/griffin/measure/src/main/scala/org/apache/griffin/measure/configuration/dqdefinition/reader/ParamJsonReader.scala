/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.griffin.measure.configuration.dqdefinition.reader

import scala.reflect.ClassTag
import scala.util.Try

import org.apache.griffin.measure.configuration.dqdefinition.Param
import org.apache.griffin.measure.utils.JsonUtil

/**
 * read params from json string directly
 *
 * @param jsonString
 */
case class ParamJsonReader(jsonString: String) extends ParamReader {

  def readConfig[T <: Param](implicit m: ClassTag[T]): Try[T] = {
    Try {
      val param = JsonUtil.fromJson[T](jsonString)
      validate(param)
    }
  }

}
