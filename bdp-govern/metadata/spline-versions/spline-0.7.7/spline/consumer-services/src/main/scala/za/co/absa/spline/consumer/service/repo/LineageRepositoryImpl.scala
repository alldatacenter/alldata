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

import java.util.concurrent.CompletionException
import com.arangodb.ArangoDBException
import com.arangodb.async.ArangoDatabaseAsync
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import za.co.absa.spline.consumer.service.model.{WriteEventInfo, LineageOverview}

import scala.PartialFunction.cond
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

@Repository
class LineageRepositoryImpl @Autowired()(db: ArangoDatabaseAsync) extends LineageRepository {

  override def lineageOverviewForExecutionEvent(eventId: WriteEventInfo.Id, maxDepth: Int)(implicit ec: ExecutionContext): Future[LineageOverview] =
    db
      .route(s"/spline/events/$eventId/lineage-overview/$maxDepth")
      .get()
      .toScala
      .map(resp => db.util().deserialize[LineageOverview](resp.getBody, classOf[LineageOverview]))
      .recover({
        case ce: CompletionException
          if cond(ce.getCause)({ case ae: ArangoDBException => ae.getResponseCode == 404 }) =>
          throw new NoSuchElementException(s"Event ID: $eventId")
      })
}
