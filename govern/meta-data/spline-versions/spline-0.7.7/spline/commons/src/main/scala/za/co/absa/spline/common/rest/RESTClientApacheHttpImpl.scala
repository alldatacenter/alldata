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

package za.co.absa.spline.common.rest

import org.apache.commons.io.IOUtils
import org.apache.http.auth.Credentials
import org.apache.http.client.methods.{HttpDelete, HttpGet, HttpPost, HttpRequestBase}
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.entity.{AbstractHttpEntity, ByteArrayEntity, StringEntity}
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.HttpClients
import za.co.absa.commons.lang.ARM
import za.co.absa.commons.lang.ARM.managed

import java.net.URI
import javax.net.ssl.SSLContext
import scala.concurrent.{ExecutionContext, Future}

class RESTClientApacheHttpImpl(
  uri: URI,
  maybeCredentials: Option[Credentials],
  maybeSslContext: Option[SSLContext])
  (implicit ec: ExecutionContext)
  extends RESTClient {

  import za.co.absa.commons.lang.OptionImplicits._

  override def get(path: String): Future[String] = execHttp {
    baseUri => new HttpGet(s"$baseUri/$path")
  }

  override def delete(path: String): Future[Unit] = execHttp {
    baseUri => new HttpDelete(s"$baseUri/$path")
  }.map(_ => {})

  override def post(path: String, body: String): Future[Unit] =
    post(path, new StringEntity(body))

  override def post(path: String, body: Array[Byte]): Future[Unit] =
    post(path, new ByteArrayEntity(body))

  private def post(path: String, entity: AbstractHttpEntity): Future[Unit] = execHttp {
    baseUri =>
      new HttpPost(s"$baseUri/$path") {
        setEntity(entity)
      }
  }.map(_ => {})

  private def execHttp(method: URI => HttpRequestBase): Future[String] = Future {
    val request = {
      val req = method(uri)
      maybeCredentials.foreach(credentials => {
        val authHeader = new BasicScheme().authenticate(credentials, req, null)
        req.addHeader(authHeader)
      })
      req
    }

    val (respStatusLine, respBody) =
      for {
        httpClient <- managed(createClient)
        response <- managed(httpClient.execute(request))
      } yield {
        val maybeBody = Option(response.getEntity)
          .map(e => {
            val encoding = Option(e.getContentEncoding).map(_.getValue).getOrElse("UTF-8")
            ARM.using(e.getContent) {
              inputStream =>
                IOUtils.toString(inputStream, encoding)
            }
          })
        (response.getStatusLine, maybeBody.orNull)
      }

    respStatusLine.getStatusCode match {
      case 200 | 201 | 204 =>
        respBody
      case _ =>
        throw new HttpStatusException(respStatusLine.getStatusCode, s"ArangoDB response: $respStatusLine. $respBody")
    }
  }

  private def createClient = {
    HttpClients
      .custom()
      .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
      .having(maybeSslContext)(_ setSSLContext _)
      .build()
  }
}
