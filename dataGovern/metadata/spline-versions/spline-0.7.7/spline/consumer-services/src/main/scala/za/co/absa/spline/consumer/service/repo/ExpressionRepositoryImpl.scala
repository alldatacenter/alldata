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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import za.co.absa.spline.consumer.service.model.ExpressionGraph
import za.co.absa.spline.consumer.service.model.Operation.Id

import scala.concurrent.{ExecutionContext, Future}

@Repository
class ExpressionRepositoryImpl @Autowired()(db: ArangoDatabaseAsync) extends ExpressionRepository {

  import za.co.absa.spline.persistence.ArangoImplicits._

  override def expressionGraphUsedByOperation(operationId: Id)(implicit ec: ExecutionContext): Future[ExpressionGraph] = {
    db.queryOne[ExpressionGraph](
      """
        |WITH operation, uses, expression, takes, attribute
        |LET op = DOCUMENT("operation", @operationId)
        |LET ps = (
        |    FOR v, e IN 1..999999
        |        OUTBOUND op uses, takes
        |        RETURN [
        |            UNSET(
        |                MERGE(v, {
        |                    "_id" : v._key
        |                }),
        |                ["_key", "_created", "_rev", "type"]
        |            ),
        |            MERGE(KEEP(e, ["index", "path"]), {
        |                "source"   : PARSE_IDENTIFIER(e._from).key,
        |                "target"   : PARSE_IDENTIFIER(e._to).key,
        |                "type"     : PARSE_IDENTIFIER(e._id).collection,
        |            })
        |        ]
        |)
        |
        |RETURN {
        |    "nodes" : UNIQUE(ps[*][0]),
        |    "edges" : UNIQUE(ps[*][1])
        |}
        |""".stripMargin,
      Map("operationId" -> operationId))
  }
}
