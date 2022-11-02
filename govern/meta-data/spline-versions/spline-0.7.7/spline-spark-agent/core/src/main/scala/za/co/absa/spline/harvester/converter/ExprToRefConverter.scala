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

package za.co.absa.spline.harvester.converter

import org.apache.spark.sql.catalyst.{expressions => sparkExprssions}
import za.co.absa.commons.lang.Converter
import za.co.absa.spline.producer.model.AttrOrExprRef

class ExprToRefConverter(
  attributeConverter: AttributeConverter,
  expressionConverter: ExpressionConverter,
  literalConverter: LiteralConverter
) extends Converter {

  override type From = sparkExprssions.Expression
  override type To = AttrOrExprRef

  override def convert(arg: sparkExprssions.Expression): AttrOrExprRef = arg match {
    case sa: sparkExprssions.Attribute =>
      val attr = attributeConverter.convert(sa)
      AttrOrExprRef(Some(attr.id), None)

    case l: sparkExprssions.Literal =>
      val literal = literalConverter.convert(l)
      AttrOrExprRef(None, Some(literal.id))

    case _ =>
      val expr = expressionConverter.convert(arg)
      AttrOrExprRef(None, Some(expr.id))
  }
}
