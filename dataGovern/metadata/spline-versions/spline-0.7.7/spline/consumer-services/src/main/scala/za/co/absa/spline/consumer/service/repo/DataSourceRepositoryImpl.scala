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

package za.co.absa.spline.consumer.service.repo

import com.arangodb.async.ArangoDatabaseAsync
import com.arangodb.model.AqlQueryOptions
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import za.co.absa.spline.consumer.service.model.DataSourceActionType.{Read, Write}
import za.co.absa.spline.consumer.service.model._
import za.co.absa.spline.persistence.ArangoImplicits._
import za.co.absa.spline.persistence.model.{EdgeDef, NodeDef}

import scala.compat.java8.StreamConverters._
import scala.concurrent.{ExecutionContext, Future}

@Repository
class DataSourceRepositoryImpl @Autowired()(db: ArangoDatabaseAsync) extends DataSourceRepository {

  override def getTimestampRange(
    asAtTime: Long,
    maybeSearchTerm: Option[String],
    maybeAppend: Option[Boolean],
    maybeApplicationId: Option[String],
    maybeDataSourceUri: Option[String]
  )(implicit ec: ExecutionContext): Future[(Long, Long)] = {
    db.queryOne[Array[Long]](
      """
        |WITH dataSource, progress
        |FOR ds IN dataSource
        |    FILTER ds._created <= @asAtTime
        |
        |    FILTER @dataSourceUri == null OR @dataSourceUri == ds.uri
        |
        |    // last write event or null
        |    LET lwe = FIRST(
        |        FOR we IN progress
        |            FILTER we._created <= @asAtTime
        |            FILTER we.execPlanDetails.dataSourceUri == ds.uri
        |            SORT we.timestamp DESC
        |            RETURN we
        |    )
        |
        |    FILTER (@applicationId  == null OR @applicationId  == lwe.extra.appId)
        |       AND (@writeAppend    == null OR @writeAppend    == lwe.execPlanDetails.append)
        |
        |    FILTER
        |        (lwe == null
        |           AND @searchTerm == null
        |           AND @applicationId == null
        |           AND @writeAppend == null
        |           AND @dataSourceUri == null
        |        )
        |        OR @searchTerm == null
        |        OR @searchTerm == lwe.timestamp
        |        OR CONTAINS(LOWER(ds.uri), @searchTerm)
        |        OR CONTAINS(LOWER(lwe.execPlanDetails.frameworkName), @searchTerm)
        |        OR CONTAINS(LOWER(lwe.execPlanDetails.applicationName), @searchTerm)
        |        OR CONTAINS(LOWER(lwe.extra.appId), @searchTerm)
        |        OR CONTAINS(LOWER(lwe.execPlanDetails.dataSourceType), @searchTerm)
        |
        |    COLLECT AGGREGATE
        |        minTimestamp = MIN(lwe.timestamp),
        |        maxTimestamp = MAX(lwe.timestamp)
        |
        |    RETURN [
        |        minTimestamp || DATE_NOW(),
        |        maxTimestamp || DATE_NOW()
        |    ]
        |""".stripMargin,
      Map(
        "asAtTime" -> Long.box(asAtTime),
        "searchTerm" -> maybeSearchTerm.map(StringUtils.lowerCase).orNull,
        "writeAppend" -> maybeAppend.map(Boolean.box).orNull,
        "applicationId" -> maybeApplicationId.orNull,
        "dataSourceUri" -> maybeDataSourceUri.orNull
      )
    ).map { case Array(from, to) => from -> to }
  }

  override def find(
    asAtTime: Long,
    maybeWriteTimestampStart: Option[Long],
    maybeWriteTimestampEnd: Option[Long],
    pageRequest: PageRequest,
    sortRequest: SortRequest,
    maybeSearchTerm: Option[String],
    maybeAppend: Option[Boolean],
    maybeWriteApplicationId: Option[String],
    maybeDataSourceUri: Option[String]
  )(implicit ec: ExecutionContext): Future[(Seq[WriteEventInfo], Long)] = {

    db.queryAs[WriteEventInfo](
      """
        |WITH progress, dataSource
        |FOR ds IN dataSource
        |    FILTER ds._created <= @asAtTime
        |
        |    FILTER @dataSourceUri == null OR @dataSourceUri == ds.uri
        |
        |    // last write event or null
        |    LET lwe = FIRST(
        |        // we're filtering by URI instead of doing traversing for the performance reasons (traversing is slower than a simple index scan)
        |        FOR we IN progress
        |            FILTER we._created <= @asAtTime
        |            FILTER we.execPlanDetails.dataSourceUri == ds.uri
        |            SORT we.timestamp DESC
        |            RETURN we
        |    )
        |
        |    FILTER (@timestampStart == null OR @timestampStart <= lwe.timestamp)
        |       AND (@timestampEnd   == null OR @timestampEnd   >= lwe.timestamp)
        |       AND (@applicationId  == null OR @applicationId  == lwe.extra.appId)
        |       AND (@writeAppend    == null OR @writeAppend    == lwe.execPlanDetails.append)
        |
        |    FILTER
        |        (lwe == null
        |           AND @searchTerm == null
        |           AND @applicationId == null
        |           AND @writeAppend == null
        |           AND @dataSourceUri == null
        |           AND @timestampStart == null
        |           AND @timestampEnd == null
        |        )
        |        OR @searchTerm == null
        |        OR @searchTerm == lwe.timestamp
        |        OR CONTAINS(LOWER(ds.uri), @searchTerm)
        |        OR CONTAINS(LOWER(lwe.execPlanDetails.frameworkName), @searchTerm)
        |        OR CONTAINS(LOWER(lwe.execPlanDetails.applicationName), @searchTerm)
        |        OR CONTAINS(LOWER(lwe.extra.appId), @searchTerm)
        |        OR CONTAINS(LOWER(lwe.execPlanDetails.dataSourceType), @searchTerm)
        |
        |    LET resItem = {
        |        "executionEventId" : lwe._key,
        |        "executionPlanId"  : lwe.execPlanDetails.executionPlanKey,
        |        "frameworkName"    : lwe.execPlanDetails.frameworkName,
        |        "applicationName"  : lwe.execPlanDetails.applicationName,
        |        "applicationId"    : lwe.extra.appId,
        |        "timestamp"        : lwe.timestamp,
        |        "dataSourceName"   : ds.name,
        |        "dataSourceUri"    : ds.uri,
        |        "dataSourceType"   : lwe.execPlanDetails.dataSourceType,
        |        "append"           : lwe.execPlanDetails.append,
        |        "durationNs"       : lwe.durationNs
        |    }
        |
        |    SORT resItem.@sortField @sortOrder
        |    LIMIT @pageOffset*@pageSize, @pageSize
        |
        |    RETURN resItem
        |""".stripMargin,
      Map(
        "asAtTime" -> Long.box(asAtTime),
        "timestampStart" -> maybeWriteTimestampStart.map(Long.box).orNull,
        "timestampEnd" -> maybeWriteTimestampEnd.map(Long.box).orNull,
        "pageOffset" -> Int.box(pageRequest.page - 1),
        "pageSize" -> Int.box(pageRequest.size),
        "sortField" -> sortRequest.sortField,
        "sortOrder" -> sortRequest.sortOrder,
        "searchTerm" -> maybeSearchTerm.map(StringUtils.lowerCase).orNull,
        "writeAppend" -> maybeAppend.map(Boolean.box).orNull,
        "applicationId" -> maybeWriteApplicationId.orNull,
        "dataSourceUri" -> maybeDataSourceUri.orNull
      ),
      new AqlQueryOptions().fullCount(true)
    ).map {
      arangoCursorAsync =>
        val items = arangoCursorAsync.streamRemaining().toScala
        val totalCount = arangoCursorAsync.getStats.getFullCount
        items -> totalCount
    }
  }

  override def findByUsage(
    execPlanId: ExecutionPlanInfo.Id,
    access: Option[DataSourceActionType]
  )(implicit ec: ExecutionContext): Future[Array[String]] = {
    access
      .map({
        case Read => db.queryStream[String](
          s"""
             |WITH ${NodeDef.DataSource.name}, ${EdgeDef.Depends.name}
             |FOR ds IN 1..1
             |    OUTBOUND DOCUMENT('executionPlan', @planId) depends
             |    RETURN ds.uri
             |""".stripMargin,
          Map("planId" -> execPlanId)
        ).map(_.toArray)

        case Write => db.queryStream[String](
          s"""
             |WITH ${NodeDef.DataSource.name}, ${EdgeDef.Affects.name}
             |FOR ds IN 1..1
             |    OUTBOUND DOCUMENT('executionPlan', @planId) affects
             |    RETURN ds.uri
             |""".stripMargin,
          Map("planId" -> execPlanId)
        ).map(_.toArray)
      })
      .getOrElse({
        db.queryStream[String](
          s"""
             |WITH ${NodeDef.DataSource.name}, ${EdgeDef.Depends.name}, ${EdgeDef.Affects.name}
             |FOR ds IN 1..1
             |    OUTBOUND DOCUMENT('executionPlan', @planId) affects, depends
             |    RETURN ds.uri
             |""".stripMargin,
          Map("planId" -> execPlanId)
        ).map(_.toArray)
      })
  }
}
