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
package za.co.absa.spline.consumer.service.repo

import za.co.absa.spline.consumer.service.model.{PageRequest, SortRequest, WriteEventInfo}

import scala.concurrent.{ExecutionContext, Future}

trait AbstractExecutionEventRepository {

  def getTimestampRange(
    asAtTime: Long,
    maybeSearchTerm: Option[String],
    maybeAppend: Option[Boolean],
    maybeApplicationId: Option[String],
    maybeDataSourceUri: Option[String])
    (implicit ec: ExecutionContext): Future[(Long, Long)]

  def find(
    asAtTime: Long,
    maybeTimestampStart: Option[Long],
    maybeTimestampEnd: Option[Long],
    pageRequest: PageRequest,
    sortRequest: SortRequest,
    maybeSearchTerm: Option[String],
    maybeAppend: Option[Boolean],
    maybeApplicationId: Option[String],
    maybeDataSourceUri: Option[String])
    (implicit ec: ExecutionContext): Future[(Seq[WriteEventInfo], Long)]
}
