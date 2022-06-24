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
import org.apache.atlas.v1.model.instance.Referenceable
import za.co.absa.spline.persistence.api.composition.dt.DataType
import za.co.absa.spline.persistence.api.{composition => splineModel}
import za.co.absa.spline.persistence.atlas.{model => atlasModel}

/**
  * The class is responsible for conversion of [[DataType Spline data types]] to [[za.co.absa.spline.persistence.atlas.model.DataType Atlas data types]].
  */
object DataTypeConverter {

  /**
    * The method converts [[DataType Spline data types]] to [[za.co.absa.spline.persistence.atlas.model.DataType Atlas data types]].
    * @param splineTypes Spline data types to be converted
    * @return Atlas data types
    */
  def convert(splineTypes: Seq[DataType]): Seq[Referenceable with atlasModel.DataType] = {
    val atlasTypes = splineTypes.map{
      case splineModel.dt.Simple(id, name, nullable) => new atlasModel.SimpleDataType(name, id, nullable)
      case splineModel.dt.Struct(id, fields, nullable) => new atlasModel.StructDataType(convert(fields, id), id, nullable)
      case splineModel.dt.Array(id, elementDataTypeId, nullable) => new atlasModel.ArrayDataType(elementDataTypeId, id, nullable)
    }
    val idAndNameMapping = atlasTypes.map(i => i.qualifiedName -> (i.getId, i.name)).toMap
    atlasTypes.foreach(_.resolveIds(idAndNameMapping))
    atlasTypes
  }

  private def convert(structFields: Seq[splineModel.dt.StructField], typeQualifiedName: UUID): Seq[atlasModel.StructField] = {
    structFields.map{
      case splineModel.dt.StructField(name, dataTypeId) =>
        val qn = s"${typeQualifiedName}_field@$name"
        new atlasModel.StructField(name, qn, dataTypeId)
    }
  }
}
