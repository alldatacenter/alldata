/*
 * Copyright 2020 ABSA Group Limited
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

package za.co.absa.spline.producer.modelmapper.v1.spark

import za.co.absa.spline.producer.model.v1_1
import za.co.absa.spline.producer.modelmapper.v1.{AttributeConverter, FieldNamesV1, TypesV1}

class SparkSplineAttributeConverter extends AttributeConverter {

  override def convert(attrDef: TypesV1.AttrDef): v1_1.Attribute = {
    val childRefs = attrDef
      .getOrElse(FieldNamesV1.AttributeDef.Dependencies, Nil)
      .asInstanceOf[Seq[TypesV1.AttrId]]
      .map(v1_1.AttrOrExprRef.attrRef)


    val attrId = attrDef(FieldNamesV1.AttributeDef.Id).toString
    val attrName = attrDef(FieldNamesV1.AttributeDef.Name).toString
    val maybeDataType = attrDef.get(FieldNamesV1.AttributeDef.DataTypeId)

    v1_1.Attribute(
      id = attrId,
      name = attrName,
      childRefs = childRefs,
      dataType = maybeDataType,
      extra = Map.empty
    )
  }
}
