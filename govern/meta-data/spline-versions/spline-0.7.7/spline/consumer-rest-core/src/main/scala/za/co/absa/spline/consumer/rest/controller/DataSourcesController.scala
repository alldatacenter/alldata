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
import za.co.absa.spline.consumer.rest.controller.DataSourcesController.PageableDataSourcesResponse
import za.co.absa.spline.consumer.service.model._
import za.co.absa.spline.consumer.service.repo.DataSourceRepository

import scala.concurrent.Future

@RestController
@Api(tags = Array("data-sources"))
class DataSourcesController @Autowired()(
  val dataSourceRepo: DataSourceRepository
) extends AbstractExecutionEventsController(dataSourceRepo) {

  @GetMapping(Array("/data-sources"))
  @ApiOperation(
    value = "Get data sources",
    notes = "Returns a pageable list of data sources filtered by the query parameters",
    response = classOf[PageableDataSourcesResponse]
  )
  def dataSources(
    @ApiParam(value = "Beginning of the last write time range (inclusive)", example = "0")
    @RequestParam(value = "timestampStart", required = false) writeTimestampStart: java.lang.Long,

    @ApiParam(value = "End of the last write time range (inclusive)", example = "0")
    @RequestParam(value = "timestampEnd", required = false) writeTimestampEnd: java.lang.Long,

    @ApiParam(value = "Enable 'timestamp' facet computation. (default - `false`)", example = "0")
    @RequestParam(value = "facet.timestamp", defaultValue = "false") facetTimestampEnabled: Boolean,

    @ApiParam(value = "Timestamp of the request, if asAtTime equals 0, the current timestamp will be applied", example = "0")
    @RequestParam(value = "asAtTime", defaultValue = "0") asAtTime0: Long,

    @ApiParam(value = "Page number", example = "1")
    @RequestParam(value = "pageNum", defaultValue = "1") pageNum: Int,

    @ApiParam(value = "Page size", example = "0")
    @RequestParam(value = "pageSize", defaultValue = "10") pageSize: Int,

    @ApiParam(value = "Sort field")
    @RequestParam(value = "sortField", defaultValue = "dataSourceUri") sortField: String,

    @ApiParam(value = "Sort order", example = "asc")
    @RequestParam(value = "sortOrder", defaultValue = "asc") sortOrder: String,

    @ApiParam(value = "Text to filter the results")
    @RequestParam(value = "searchTerm", required = false) searchTerm: String,

    @ApiParam(value = "Write mode (true - append, false - overwrite")
    @RequestParam(value = "append", required = false) append: java.lang.Boolean,

    @ApiParam(value = "Id of the application")
    @RequestParam(value = "applicationId", required = false) writeApplicationId: String,

    @ApiParam(value = "Destination path")
    @RequestParam(value = "dataSourceUri", required = false) dataSourceUri: String

  ): Future[PageableDataSourcesResponse] = find(
    writeTimestampStart,
    writeTimestampEnd,
    facetTimestampEnabled,
    asAtTime0,
    pageNum,
    pageSize,
    sortField,
    sortOrder,
    searchTerm,
    append,
    writeApplicationId,
    dataSourceUri
  )
}

object DataSourcesController {
  // for now we reuse the data structure of the ExecutionEventController
  type PageableDataSourcesResponse = PageableExecutionEventsResponse
}
