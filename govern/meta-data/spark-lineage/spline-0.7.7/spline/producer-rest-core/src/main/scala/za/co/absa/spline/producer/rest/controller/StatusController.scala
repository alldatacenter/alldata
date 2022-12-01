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

package za.co.absa.spline.producer.rest.controller

import io.swagger.annotations.{Api, ApiOperation, ApiResponse, ApiResponses}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, ResponseEntity}
import org.springframework.web.bind.annotation._
import za.co.absa.spline.producer.rest.HttpConstants.{Encoding, SplineHeaders}
import za.co.absa.spline.producer.rest.ProducerAPI
import za.co.absa.spline.producer.service.repo.ExecutionProducerRepository

import scala.concurrent.{ExecutionContext, Future}

@RestController
@Api(tags = Array("status"))
class StatusController @Autowired()(
  val repo: ExecutionProducerRepository) {

  import ExecutionContext.Implicits.global

  @RequestMapping(
    path = Array("/status"),
    method = Array(RequestMethod.HEAD))
  @ApiOperation(
    value = "Server health status",
    notes = "Check that producer is running and that the database is accessible and initialized")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Everything's working"),
    new ApiResponse(code = 503, message = "There is a problem")
  ))
  @deprecated("Use liveness probe instead", since = "0.7.0")
  def statusHead(): Future[_] = repo
    .isDatabaseOk
    .map {
      if (_) HttpStatus.OK
      else HttpStatus.SERVICE_UNAVAILABLE
    }
    .map {
      ResponseEntity
        .status(_)
        .header(SplineHeaders.ApiVersion, ProducerAPI.SupportedVersions.map(_.asString): _*)
        .header(SplineHeaders.ApiLTSVersion, ProducerAPI.LTSVersions.map(_.asString): _*)
        .header(SplineHeaders.AcceptRequestEncoding, Encoding.GZIP)
        .build()
    }
}
