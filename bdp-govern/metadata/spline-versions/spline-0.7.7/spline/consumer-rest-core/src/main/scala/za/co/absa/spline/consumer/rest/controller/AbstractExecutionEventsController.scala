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

import za.co.absa.spline.consumer.service.model._
import za.co.absa.spline.consumer.service.repo.AbstractExecutionEventRepository

import java.lang.System.currentTimeMillis
import scala.concurrent.Future

class AbstractExecutionEventsController(
  repo: AbstractExecutionEventRepository
) {

  import za.co.absa.commons.lang.OptionImplicits._

  import scala.concurrent.ExecutionContext.Implicits._

  protected def find(
    timestampStart: java.lang.Long,
    timestampEnd: java.lang.Long,
    facetTimestampEnabled: Boolean,
    asAtTime0: Long,
    pageNum: Int,
    pageSize: Int,
    sortField: String,
    sortOrder: String,
    searchTerm: String,
    append: java.lang.Boolean,
    applicationId: String,
    dataSourceUri: String
  ): Future[PageableExecutionEventsResponse] = {

    val asAtTime = if (asAtTime0 < 1) currentTimeMillis else asAtTime0
    val pageRequest = PageRequest(pageNum, pageSize)
    val sortRequest = SortRequest(sortField, sortOrder)

    val maybeSearchTerm = searchTerm.nonBlankOption
    val maybeAppend = append.asOption.map(Boolean.unbox)
    val maybeWriteApplicationId = applicationId.nonBlankOption
    val maybeDataSourceUri = dataSourceUri.nonBlankOption
    val maybeWriteTimestampStart = timestampStart.asOption.map(Long.unbox)
    val maybeWriteTimestampEnd = timestampEnd.asOption.map(Long.unbox)

    val eventualDateRange: Future[Array[Long]] =
      if (facetTimestampEnabled)
        repo.getTimestampRange(
          asAtTime,
          maybeSearchTerm,
          maybeAppend,
          maybeWriteApplicationId,
          maybeDataSourceUri
        ) map {
          case (totalDateFrom, totalDateTo) => Array(totalDateFrom, totalDateTo)
        }
      else
        Future.successful(Array.empty)

    val eventualEventsWithCount =
      repo.find(
        asAtTime,
        maybeWriteTimestampStart,
        maybeWriteTimestampEnd,
        pageRequest,
        sortRequest,
        maybeSearchTerm,
        maybeAppend,
        maybeWriteApplicationId,
        maybeDataSourceUri)

    for {
      totalDateRange <- eventualDateRange
      (events, totalCount) <- eventualEventsWithCount
    } yield {
      PageableExecutionEventsResponse(
        events.toArray,
        totalCount,
        pageRequest.page,
        pageRequest.size,
        totalDateRange)
    }
  }
}
