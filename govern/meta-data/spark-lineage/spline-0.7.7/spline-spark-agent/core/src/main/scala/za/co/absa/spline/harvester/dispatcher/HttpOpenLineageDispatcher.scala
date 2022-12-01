/*
 * Copyright 2019 ABSA Group Limited
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

package za.co.absa.spline.harvester.dispatcher

import org.apache.commons.configuration.Configuration
import org.apache.spark.internal.Logging
import scalaj.http.{Http, HttpStatusException}
import za.co.absa.commons.version.Version
import za.co.absa.spline.harvester.dispatcher.httpdispatcher.rest.{RestClient, RestEndpoint}
import za.co.absa.spline.harvester.dispatcher.modelmapper.{ModelMapper, OpenLineageModelMapper}
import za.co.absa.spline.harvester.dispatcher.openlineage.{HttpOpenLineageDispatcherConfig, RESTResource}
import za.co.absa.spline.producer.model.{ExecutionEvent, ExecutionPlan}

import javax.ws.rs.core.MediaType
import scala.util.control.NonFatal


class HttpOpenLineageDispatcher(restClient: RestClient, apiVersion: Version, openLineageNamespace: String)
  extends LineageDispatcher
    with Logging {

  import za.co.absa.spline.harvester.json.HarvesterJsonSerDe.impl._

  def this(dispatcherConfig: HttpOpenLineageDispatcherConfig) = this(
    HttpOpenLineageDispatcher.createDefaultRestClient(dispatcherConfig),
    dispatcherConfig.apiVersion,
    dispatcherConfig.namespace
  )

  def this(configuration: Configuration) = this(new HttpOpenLineageDispatcherConfig(configuration))


  override def name = "Open Lineage Http"

  logInfo(s"Using Producer API version: ${apiVersion.asString}")

  private val lineageEndpoint = restClient.endpoint(RESTResource.Lineage)
  private val modelMapper = ModelMapper.forApiVersion(apiVersion)
  private val openLineageModelMapper = new OpenLineageModelMapper(modelMapper, apiVersion, openLineageNamespace)

  private var cachedPlan: ExecutionPlan = _

  override def send(plan: ExecutionPlan): Unit = {
    cachedPlan = plan
  }

  override def send(event: ExecutionEvent): Unit = {
    assert(cachedPlan != null)
    val plan = cachedPlan

    val runEvents = openLineageModelMapper.toDtos(plan, event)

    runEvents.foreach { event =>
      sendJson(event.toJson, lineageEndpoint)
    }
  }

  private def sendJson(json: String, endpoint: RestEndpoint): Unit = {
    val url = endpoint.request.url
    logTrace(s"sendJson $url : \n${json.asPrettyJson}")

    try {
      endpoint
        .post(json, MediaType.APPLICATION_JSON, false)
        .throwError

    } catch {
      case HttpStatusException(code, _, body) =>
        throw new RuntimeException(HttpOpenLineageDispatcher.createHttpErrorMessage(s"Cannot send lineage data to $url", code, body))
      case NonFatal(e) =>
        throw new RuntimeException(s"Cannot send lineage data to $url", e)
    }
  }
}

object HttpOpenLineageDispatcher extends Logging {
  
  private def createDefaultRestClient(config: HttpOpenLineageDispatcherConfig): RestClient = {
    logInfo(s"Producer URL: ${config.apiUrl}")
    RestClient(
      Http,
      config.apiUrl,
      config.connTimeout,
      config.readTimeout
    )
  }

  private def createHttpErrorMessage(msg: String, code: Int, body: String): String = {
    s"$msg. HTTP Response: $code $body"
  }
}
