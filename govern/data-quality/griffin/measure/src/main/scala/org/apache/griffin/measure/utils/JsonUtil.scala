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

package org.apache.griffin.measure.utils

import java.io.InputStream

import scala.reflect._

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.commons.lang3.StringEscapeUtils

object JsonUtil {
  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.configure(SerializationFeature.INDENT_OUTPUT, true)

  def toJson(value: Map[Symbol, Any]): String = {
    toJson(value map { case (k, v) => k.name -> v })
  }

  def toJson(value: Any): String = {
    StringEscapeUtils.unescapeJava(mapper.writeValueAsString(value))
  }

  def fromJson[T: ClassTag](json: String): T = {
    mapper.readValue[T](json, classTag[T].runtimeClass.asInstanceOf[Class[T]])
  }

  def fromJson[T: ClassTag](is: InputStream): T = {
    mapper.readValue[T](is, classTag[T].runtimeClass.asInstanceOf[Class[T]])
  }

  def toAnyMap(json: String): Map[String, Any] = {
    mapper.readValue(json, classOf[Map[String, Any]])
  }
}
