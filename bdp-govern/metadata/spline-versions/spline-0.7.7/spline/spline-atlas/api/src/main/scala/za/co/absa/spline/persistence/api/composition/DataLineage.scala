/*
 * Copyright 2017 ABSA Group Limited
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

package za.co.absa.spline.persistence.api.composition

import java.util.UUID
import salat.annotations.Persist
import za.co.absa.spline.persistence.api.composition.dt.DataType
import za.co.absa.spline.persistence.api.composition.expr.{Expression, TypedExpression}
import za.co.absa.spline.persistence.api.composition.op.{ExpressionAware, Operation}

/**
  * The case class represents a partial data lineage graph of a Spark dataset(s).
  *
  * @param appId      An unique identifier of the application run
  * @param appName    A name of the Spark application
  * @param timestamp  A timestamp describing when the application was executed
  * @param operations A sequence of nodes representing the data lineage graph
  * @param datasets   A sequence of data sets produced or consumed by operations
  * @param attributes A sequence of attributes contained in schemas of data sets
  */
case class DataLineage
(
  appId: String,
  appName: String,
  timestamp: Long,
  sparkVer: String,
  operations: Seq[Operation],
  datasets: Seq[MetaDataset],
  attributes: Seq[Attribute],
  dataTypes: Seq[DataType],
  writeIgnored: Boolean = false
) {
  require(operations.nonEmpty, "list of operations cannot be empty")
  require(datasets.nonEmpty, "list of datasets cannot be empty")
  require(rootOperation.mainProps.output == rootDataset.id)

  /**
    * A unique identifier of the data lineage
    */
  @Persist
  lazy val id: String = DataLineageId.fromDatasetId(rootDataset.id)

  /**
    * A node representing the last operation performed within data lineage graph. Usually, it describes persistence of data set to some file, database, Kafka endpoint, etc.
    */
  @Persist
  lazy val rootOperation: Operation = operations.head

  /**
    * A descriptor of the data set produced by the computation.
    */
  @Persist
  lazy val rootDataset: MetaDataset = datasets.head

  /**
    * Returns a copy of this [[DataLineage]] with all unused components removed.
    * A component is considered unused if:
    * - for a [[MetaDataset]], it's not declared as an input or output of any [[Operation]] in this lineage instance
    * - for an [[Attribute]], it's not listed in any [[MetaDataset]]'s schema
    * - for a [[DataType]], it's not referred from any [[Attribute]], [[Expression]] or another parent [[DataType]]
    * that itself isn't unused.
    */
  def rectified: DataLineage = DataLineage.rectify(this)
}

object DataLineage {
  private def rectify(lineage: DataLineage): DataLineage = {
    val operations = lineage.operations

    val datasets = {
      val datasetIds =
        (operations.flatMap(_.mainProps.inputs)
          ++ operations.map(_.mainProps.output)).toSet
      lineage.datasets.filter(ds => datasetIds(ds.id))
    }

    val attributes = {
      val attributeIds = datasets.flatMap(_.schema.attrs).toSet
      lineage.attributes.filter(attr => attributeIds(attr.id))
    }

    val dataTypes = {
      val expressions = operations.flatMap {
        case op: ExpressionAware => op.expressions
        case _ => Nil
      }

      val expressionTypeIds: Set[UUID] = {
        def traverseAndCollect(accumulator: Set[UUID], expressions: List[Expression]): Set[UUID] = expressions match {
          case Nil => accumulator
          case exp :: queue =>
            val updatedAccumulator = exp match {
              case tex: TypedExpression => accumulator + tex.dataTypeId
              case _ => accumulator
            }
            val updatedQueue = exp.children.toList ++ queue
            traverseAndCollect(updatedAccumulator, updatedQueue)
        }

        traverseAndCollect(Set.empty, expressions.toList)
      }

      val retainedDataTypes = {
        val allDataTypesById = lineage.dataTypes.map(dt => dt.id -> dt).toMap

        def traverseAndCollectWithChildren(accumulator: Set[UUID], typeIds: List[UUID]): Set[UUID] = typeIds match {
          case Nil => accumulator
          case dtId :: queue => traverseAndCollectWithChildren(
            accumulator = accumulator + dtId,
            typeIds = allDataTypesById(dtId).childDataTypeIds.toList ++ queue
          )
        }

        val referredTypeIds = expressionTypeIds ++ attributes.map(_.dataTypeId)
        val retainedTypeIds = traverseAndCollectWithChildren(Set.empty, referredTypeIds.toList)

        allDataTypesById.filterKeys(retainedTypeIds).values
      }

      retainedDataTypes.toSeq
    }

    lineage.copy(
      operations = operations,
      datasets = datasets,
      attributes = attributes,
      dataTypes = dataTypes)
  }
}

object DataLineageId {
  private val prefix = "ln_"

  def fromDatasetId(dsId: UUID): String = prefix + dsId

  def toDatasetId(lnId: String): UUID = UUID.fromString(lnId.substring(prefix.length))
}
