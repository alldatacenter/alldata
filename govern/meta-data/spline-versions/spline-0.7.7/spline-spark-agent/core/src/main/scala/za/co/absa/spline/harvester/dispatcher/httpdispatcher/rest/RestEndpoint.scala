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

package za.co.absa.spline.harvester.dispatcher.httpdispatcher.rest

import org.apache.http.HttpHeaders
import scalaj.http.{HttpRequest, HttpResponse}
import za.co.absa.commons.lang.ARM.using
import za.co.absa.spline.harvester.dispatcher.httpdispatcher.HttpConstants.Encoding
import za.co.absa.spline.harvester.dispatcher.httpdispatcher.rest.RestEndpoint._

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import javax.ws.rs.HttpMethod

class RestEndpoint(val request: HttpRequest) {

  def head(): HttpResponse[String] = request
    .method(HttpMethod.HEAD)
    .asString

  def post(data: String, contentType: String, enableRequestCompression: Boolean): HttpResponse[String] = {
    val jsonRequest = request
      .header(HttpHeaders.CONTENT_TYPE, contentType)

    if (enableRequestCompression && data.length > GzipCompressionLengthThreshold) {
      jsonRequest
        .header(HttpHeaders.CONTENT_ENCODING, Encoding.GZIP)
        .postData(gzipContent(data.getBytes("UTF-8")))
        .asString
    } else {
      jsonRequest
        .postData(data)
        .asString
    }
  }
}

object RestEndpoint {
  private val GzipCompressionLengthThreshold: Int = 2048

  private def gzipContent(bytes: Array[Byte]): Array[Byte] = {
    val byteStream = new ByteArrayOutputStream(bytes.length)
    using(new GZIPOutputStream(byteStream))(_.write(bytes))
    byteStream.toByteArray
  }
}
