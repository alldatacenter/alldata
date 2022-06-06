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

package com.platform.quality.configuration.dqdefinition.reader

import com.platform.quality.configuration.dqdefinition.Param
import com.platform.quality.utils.{HttpUtil, JsonUtil}

import scala.reflect.ClassTag
import scala.util.Try

/**
 * read params by http url
 *
 * @param httpUrl
 */
case class ParamHttpReader(httpUrl: String) extends ParamReader {

  def readConfig[T <: Param](implicit m: ClassTag[T]): Try[T] = {
    Try {
      val params = Map[String, Object]()
      val headers = Map[String, Object](("Content-Type", "application/json"))
      val jsonString = HttpUtil.doHttpRequest(httpUrl, "get", params, headers, null)._2
      val param = JsonUtil.fromJson[T](jsonString)
      validate(param)
    }
  }
}
