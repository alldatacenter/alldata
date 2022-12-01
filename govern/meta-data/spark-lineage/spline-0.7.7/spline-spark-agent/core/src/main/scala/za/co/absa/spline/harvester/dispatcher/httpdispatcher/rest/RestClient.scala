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

import org.apache.spark.internal.Logging
import scalaj.http.BaseHttp
import za.co.absa.spline.harvester.dispatcher.SplineHeaders

import scala.concurrent.duration.Duration

trait RestClient {
  def endpoint(url: String): RestEndpoint
}

object RestClient extends Logging {
  /**
   * @param baseHttp          HTTP client
   * @param baseURL           REST endpoint base URL
   * @param connectionTimeout timeout for establishing TCP connection
   * @param readTimeout       timeout for each individual TCP packet (in already established connection)
   */
  def apply(
    baseHttp: BaseHttp,
    baseURL: String,
    connectionTimeout: Duration,
    readTimeout: Duration): RestClient = {

    logDebug(s"baseURL = $baseURL")
    logDebug(s"connectionTimeout = $connectionTimeout")
    logDebug(s"readTimeout = $readTimeout")

    //noinspection ConvertExpressionToSAM
    new RestClient {
      override def endpoint(resource: String): RestEndpoint = new RestEndpoint(
        baseHttp(s"$baseURL/$resource")
          .timeout(connectionTimeout.toMillis.toInt, readTimeout.toMillis.toInt)
          .header(SplineHeaders.Timeout, readTimeout.toMillis.toString)
          .compress(true))
    }
  }
}
