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

package za.co.absa.spline.persistence.atlas.conversion

import java.util.UUID
import org.apache.atlas.v1.model.instance.{Id, Referenceable}
import za.co.absa.spline.persistence.api.composition.DataLineage
import za.co.absa.spline.persistence.atlas.model._


/**
  * The object is responsible for conversion of a [[DataLineage Spline lineage model]] to Atlas entities.
  */
object DataLineageToTypeSystemConverter {

  /**
    * The method converts a [[DataLineage Spline lineage model]] to Atlas entities.
    * @param lineage An input Spline lineage model
    * @return Atlas entities
    */
  def convert(lineage: DataLineage): Seq[Referenceable] = {
    val dataTypes = DataTypeConverter.convert(lineage.dataTypes)
    val dataTypeIdMap = toIdMap(dataTypes)
    val dataTypeIdAndNameMap = dataTypes.map(i => i.qualifiedName -> (i.getId, i.name)).toMap
    val attributes = AttributeConverter.convert(lineage.attributes, dataTypeIdAndNameMap)
    val attributesIdMap = toIdMap(attributes)
    val dataSets = DatasetConverter.convert(lineage, dataTypeIdMap, attributesIdMap)
    val dataSetIdMap = toIdMap(dataSets)
    val splineAttributesMap = lineage.attributes.map(i => i.id -> i).toMap
    val expressionConverter = new ExpressionConverter(splineAttributesMap, dataTypeIdMap)
    val operations = new OperationConverter(expressionConverter).convert(lineage, dataSetIdMap, attributesIdMap, dataTypeIdMap)
    val process = createProcess(lineage, operations, dataSets)
    dataTypes ++ attributes ++ dataSets ++ operations :+ process
  }

  private def toIdMap(collection: Seq[Referenceable with QualifiedEntity]): Map[UUID, Id] = {
    collection.map(i => i.qualifiedName -> i.getId).toMap
  }

  private def createProcess(lineage: DataLineage, operations : Seq[Operation] , datasets : Seq[Dataset]) : Referenceable = {
    val (inputDatasets, outputDatasets) = datasets
      .filter(_.isInstanceOf[EndpointDataset])
      .map(_.asInstanceOf[EndpointDataset])
      .partition(_.direction == EndpointDirection.input)

    new Job(
      lineage.id,
      lineage.appName,
      lineage.id,
      operations.map(_.getId),
      datasets.map(_.getId),
      inputDatasets.map(_.getId),
      outputDatasets.map(_.getId),
      inputDatasets.map(_.endpoint.getId),
      outputDatasets.map(_.endpoint.getId)
    )
  }
}
