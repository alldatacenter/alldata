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

import org.apache.atlas.v1.model.instance.Id

import za.co.absa.spline.persistence.api.composition.{DataLineage, op}
import za.co.absa.spline.persistence.atlas.model._

/**
  * The object is responsible for extraction of [[za.co.absa.spline.persistence.atlas.model.Dataset Atlas data sets]] from [[DataLineage Spline lineage]].
  */
object DatasetConverter {
  val datasetSuffix = "_Dataset"

  /**
    * The method extracts [[za.co.absa.spline.persistence.atlas.model.Dataset Atlas data sets]] from [[DataLineage Spline linage]].
    * @param lineage An input lineage
    * @param dataTypeIdMap A mapping from Spline data type ids to ids assigned by Atlas API.
    * @param attributeIdMap A mapping from Spline attribute ids to ids assigned by Atlas API.
    * @return Extracted data sets
    */
  def convert(lineage: DataLineage, dataTypeIdMap: Map[UUID, Id], attributeIdMap: Map[UUID, Id]): Seq[Dataset] = {
    for {
      operation <- lineage.operations
      dataset <- lineage.datasets if dataset.id == operation.mainProps.output
    } yield {
      val name = operation.mainProps.name + datasetSuffix
      val qualifiedName = dataset.id
      val attributes = dataset.schema.attrs.map(attributeIdMap)
      operation match {
        case op.Read(_, st, paths) =>
          val path = paths.map(_.path) mkString ", "
          new EndpointDataset(name, qualifiedName, attributes, new FileEndpoint(path, path), EndpointType.file, EndpointDirection.input, st)
        case op.Write(_, dt, path, _, _, _) => new EndpointDataset(name, qualifiedName, attributes, new FileEndpoint(path, path), EndpointType.file, EndpointDirection.output, dt)
        case _ => new Dataset(name, qualifiedName, attributes)
      }
    }
  }
}
