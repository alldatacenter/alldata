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
package za.co.absa.spline.consumer.rest.controller

import io.swagger.annotations.{Api, ApiOperation, ApiParam}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation._
import za.co.absa.spline.consumer.service.model._
import za.co.absa.spline.consumer.service.repo.ExecutionEventRepository

import scala.concurrent.Future

@RestController
@Api(tags = Array("execution-events"))
class ExecutionEventsController @Autowired()(
  val executionEventRepo: ExecutionEventRepository
) extends AbstractExecutionEventsController(executionEventRepo) {

  @GetMapping(Array("/execution-events"))
  @ApiOperation(
    value = "Get execution events",
    notes = "Returns a pageable list of execution events filtered by the query parameters",
    response = classOf[PageableExecutionEventsResponse]
  )
  def executionEvents(
    @ApiParam(value = "Beginning of the time range (inclusive)", example = "0")
    @RequestParam(value = "timestampStart", required = false) timestampStart: java.lang.Long,

    @ApiParam(value = "End of the time range (inclusive)", example = "0")
    @RequestParam(value = "timestampEnd", required = false) timestampEnd: java.lang.Long,

    @ApiParam(value = "Enable 'timestamp' facet computation. (default - `false`)", example = "0")
    @RequestParam(value = "facet.timestamp", defaultValue = "false") facetTimestampEnabled: Boolean,

    @ApiParam(value = "Timestamp of the request, if asAtTime equals 0, the current timestamp will be applied", example = "0")
    @RequestParam(value = "asAtTime", defaultValue = "0") asAtTime0: Long,

    @ApiParam(value = "Page number", example = "1")
    @RequestParam(value = "pageNum", defaultValue = "1") pageNum: Int,

    @ApiParam(value = "Page size", example = "0")
    @RequestParam(value = "pageSize", defaultValue = "10") pageSize: Int,

    @ApiParam(value = "Sort field")
    @RequestParam(value = "sortField", defaultValue = "timestamp") sortField: String,

    @ApiParam(value = "Sort order", example = "asc")
    @RequestParam(value = "sortOrder", defaultValue = "desc") sortOrder: String,

    @ApiParam(value = "Text to filter the results")
    @RequestParam(value = "searchTerm", required = false) searchTerm: String,

    @ApiParam(value = "Write mode (true - append, false - overwrite")
    @RequestParam(value = "append", required = false) append: java.lang.Boolean,

    @ApiParam(value = "Id of the application")
    @RequestParam(value = "applicationId", required = false) applicationId: String,

    @ApiParam(value = "Destination path")
    @RequestParam(value = "dataSourceUri", required = false) dataSourceUri: String

  ): Future[PageableExecutionEventsResponse] = find(
    timestampStart,
    timestampEnd,
    facetTimestampEnabled,
    asAtTime0,
    pageNum,
    pageSize,
    sortField,
    sortOrder,
    searchTerm,
    append,
    applicationId,
    dataSourceUri
  )
}
