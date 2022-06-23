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

import com.arangodb.async.ArangoDatabaseAsync
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import za.co.absa.spline.consumer.service.model.{Operation, OperationDetails}

import scala.concurrent.{ExecutionContext, Future}

@Repository
class OperationRepositoryImpl @Autowired()(db: ArangoDatabaseAsync) extends OperationRepository {

  import za.co.absa.spline.persistence.ArangoImplicits._

  override def findById(operationId: Operation.Id)(implicit ec: ExecutionContext): Future[OperationDetails] = {
    db.queryOne[OperationDetails](
      """
        |WITH executionPlan, executes, operation, follows, emits, schema, consistsOf, uses, takes, attribute, expression
        |FOR ope IN operation
        |    FILTER ope._key == @operationId
        |
        |    LET inputs = (
        |        FOR v, e IN 1..1
        |            OUTBOUND ope follows
        |            SORT e.index
        |            RETURN v
        |    )
        |
        |    LET schemas = (
        |        FOR op IN APPEND(inputs, ope)
        |            LET schema = (
        |                FOR attr, e IN 2 OUTBOUND op emits, consistsOf
        |                    SORT e.index
        |                    RETURN {
        |                        "id": attr._key,
        |                        "name": attr.name,
        |                        "dataTypeId": attr.dataType
        |                    }
        |            )
        |            RETURN schema
        |    )
        |
        |    LET dataTypesFormatted = (
        |        LET execPlan = DOCUMENT(ope._belongsTo)
        |        FOR d IN execPlan.extra.dataTypes || []
        |            RETURN MERGE(
        |                KEEP(d,  "id", "name", "fields", "nullable", "elementDataTypeId"),
        |                {
        |                    "_class": d._typeHint == "dt.Simple" ?  "za.co.absa.spline.consumer.service.model.SimpleDataType"
        |                            : d._typeHint == "dt.Array"  ?  "za.co.absa.spline.consumer.service.model.ArrayDataType"
        |                            :                               "za.co.absa.spline.consumer.service.model.StructDataType"
        |                }
        |            )
        |    )
        |
        |    RETURN {
        |        "operation": {
        |            "_id"       : ope._key,
        |            "_type"     : ope.type,
        |            "name"      : ope.name || ope.type,
        |            "properties": MERGE(
        |                {
        |                    "inputSources": ope.inputSources,
        |                    "outputSource": ope.outputSource,
        |                    "append"      : ope.append
        |                },
        |                ope.params,
        |                ope.extra
        |            )
        |        },
        |        "dataTypes": dataTypesFormatted,
        |        "schemas"  : schemas,
        |        "inputs"   : LENGTH(inputs) > 0 ? RANGE(0, LENGTH(inputs) - 1) : [],
        |        "output"   : LENGTH(schemas) - 1
        |    }
        |""".stripMargin,
      Map("operationId" -> operationId)
    )
  }
}
