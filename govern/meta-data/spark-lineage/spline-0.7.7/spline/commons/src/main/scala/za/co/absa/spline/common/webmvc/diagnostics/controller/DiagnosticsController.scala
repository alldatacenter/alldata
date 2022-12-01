/*
 * Copyright 2021 ABSA Group Limited
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

package za.co.absa.spline.common.webmvc.diagnostics.controller

import com.typesafe.config.{ConfigFactory, ConfigRenderOptions}
import io.swagger.annotations.ApiOperation
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.{GetMapping, RestController}
import za.co.absa.spline.common.SplineBuildInfo

@RestController
class DiagnosticsController {

  @GetMapping(path = Array("/version"), produces = Array(APPLICATION_JSON_VALUE))
  @ApiOperation("Get application version info")
  def buildInfo: String = ConfigFactory
    .parseProperties(SplineBuildInfo.BuildProps)
    .root()
    .render(ConfigRenderOptions.concise)

  @GetMapping(Array("/readiness"))
  @ApiOperation("Service 'readiness' probe")
  def readinessProbe: ResponseEntity[_] = ResponseEntity.ok(null)

  @GetMapping(Array("/liveness"))
  @ApiOperation("Service 'liveness' probe")
  def livenessProbe: ResponseEntity[_] = ResponseEntity.ok(null)
}
