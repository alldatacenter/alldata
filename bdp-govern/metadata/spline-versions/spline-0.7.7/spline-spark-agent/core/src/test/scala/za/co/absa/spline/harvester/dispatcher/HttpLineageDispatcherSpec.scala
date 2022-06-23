/*
 * Copyright 2019 ABSA Group Limited
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

package za.co.absa.spline.harvester.dispatcher

import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.mockito.MockitoSugar
import scalaj.http._
import za.co.absa.spline.harvester.dispatcher.httpdispatcher.rest.{RestClient, RestEndpoint}
import za.co.absa.spline.harvester.exception.SplineInitializationException

class HttpLineageDispatcherSpec extends AnyFlatSpec with MockitoSugar {

  behavior of "HttpLineageDispatcher"

  private val restClientMock = mock[RestClient]
  private val httpRequestMock = mock[HttpRequest]
  private val httpResponseMock = mock[HttpResponse[String]]

  when(restClientMock.endpoint("status")) thenReturn new RestEndpoint(httpRequestMock)
  when(httpRequestMock.method("HEAD")) thenReturn httpRequestMock

  it should "not do anything when producer is ready" in {
    when(httpRequestMock.asString) thenReturn httpResponseMock
    when(httpResponseMock.is2xx) thenReturn true
    when(httpResponseMock.headers) thenReturn Map.empty[String, IndexedSeq[String]]

    new HttpLineageDispatcher(restClientMock)
  }

  it should "throw when producer is not ready" in {
    when(httpRequestMock.asString) thenReturn httpResponseMock
    when(httpResponseMock.is2xx) thenReturn false
    when(httpResponseMock.is5xx) thenReturn true

    assertThrows[SplineInitializationException] {
      new HttpLineageDispatcher(restClientMock)
    }
  }

  it should "throw when connection to producer was not successful" in {
    when(httpRequestMock.asString) thenThrow new RuntimeException
    assertThrows[SplineInitializationException] {
      new HttpLineageDispatcher(restClientMock)
    }
  }
}
