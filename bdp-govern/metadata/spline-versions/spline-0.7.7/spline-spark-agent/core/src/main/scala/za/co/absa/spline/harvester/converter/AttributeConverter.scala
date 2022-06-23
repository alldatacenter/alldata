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

package za.co.absa.spline.harvester.converter

import org.apache.spark.sql.catalyst.{expressions => sparkExprssions}
import za.co.absa.commons.lang.Converter
import za.co.absa.spline.harvester.IdGenerator
import za.co.absa.spline.producer.model.Attribute

class AttributeConverter(
  idGen: IdGenerator[Any, String],
  dataTypeConverter: DataTypeConverter,
  resolveAttributeChild: sparkExprssions.Attribute => Option[sparkExprssions.Expression],
  outputExprToAttMap: Map[sparkExprssions.ExprId, Attribute],
  exprToRefConverter: => ExprToRefConverter
) extends Converter {

  override type From = sparkExprssions.Attribute
  override type To = Attribute

  def convert(expr: From): To = expr match {
    case attr: sparkExprssions.Attribute if outputExprToAttMap.contains(attr.exprId) =>
      outputExprToAttMap(attr.exprId)

    case attr: sparkExprssions.Attribute =>
      Attribute(
        id = idGen.nextId(),
        dataType = Some(dataTypeConverter.convert(attr.dataType, attr.nullable).id),
        childRefs = resolveAttributeChild(attr)
          .map(expr => Seq(exprToRefConverter.convert(expr))),
        extra = None,
        name = attr.name
      )
  }
}
