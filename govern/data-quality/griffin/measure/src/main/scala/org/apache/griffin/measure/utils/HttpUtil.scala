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

import scala.util.matching.Regex

import org.apache.http.client.methods.{HttpDelete, HttpGet, HttpPost, HttpPut}
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.{BasicResponseHandler, HttpClientBuilder}

object HttpUtil {

  val GET_REGEX: Regex = """^(?i)get$""".r
  val POST_REGEX: Regex = """^(?i)post$""".r
  val PUT_REGEX: Regex = """^(?i)put$""".r
  val DELETE_REGEX: Regex = """^(?i)delete$""".r

  def doHttpRequest(
      url: String,
      method: String,
      params: Map[String, Object],
      headers: Map[String, Object],
      data: String): (Integer, String) = {
    val client = HttpClientBuilder.create.build
    val uriBuilder = new URIBuilder(url)
    convertObjMap2StrMap(params) foreach (param => uriBuilder.setParameter(param._1, param._2))
    val handler = new BasicResponseHandler()
    val request = method match {
      case POST_REGEX() =>
        val post = new HttpPost(uriBuilder.build())
        post.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON))
        post
      case PUT_REGEX() =>
        val put = new HttpPut(uriBuilder.build())
        put.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON))
        put
      case GET_REGEX() =>
        new HttpGet(uriBuilder.build())
      case DELETE_REGEX() =>
        new HttpDelete(uriBuilder.build())
      case _ => throw new UnsupportedOperationException("Unsupported http method error!")
    }
    convertObjMap2StrMap(headers) foreach (header => request.addHeader(header._1, header._2))
    val response = client.execute(request)
    (response.getStatusLine.getStatusCode, handler.handleResponse(response).trim)
  }

  private def convertObjMap2StrMap(map: Map[String, Object]): Map[String, String] = {
    map.map(pair => pair._1 -> pair._2.toString)
  }
}
