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

import org.apache.griffin.measure.utils.JsonUtil

object ParamReaderFactory {

  val json = "json"
  val file = "file"
  val httpRegex = "^http[s]?://.*"

  /**
   * parse string content to get param reader
   * @param pathOrJson
   * @return
   */
  def getParamReader(pathOrJson: String): ParamReader = {
    if (pathOrJson.matches(httpRegex)) {
      ParamHttpReader(pathOrJson)
    } else {
      val strType = paramStrType(pathOrJson)
      if (json.equals(strType)) ParamJsonReader(pathOrJson)
      else ParamFileReader(pathOrJson)
    }
  }

  private def paramStrType(str: String): String = {
    try {
      JsonUtil.toAnyMap(str)
      json
    } catch {
      case _: Throwable => file
    }
  }

}
