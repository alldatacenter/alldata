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
import za.co.absa.spline.producer.model._

import scala.language.reflectiveCalls

class LiteralConverter(
  idGen: IdGenerator[Any, String],
  dataConverter: DataConverter,
  dataTypeConverter: DataTypeConverter
) extends Converter {

  import ExpressionConverter._
  import za.co.absa.commons.lang.OptionImplicits._

  override type From = sparkExprssions.Literal
  override type To = Literal

  def convert(lit: From): To =
    Literal(
      id = idGen.nextId(),
      dataType = dataTypeConverter
        .convert(lit.dataType, lit.nullable)
        .asOption
        .map(_.id),
      extra = createExtra(lit, "expr.Literal").asOption,
      value = dataConverter.convert((lit.value, lit.dataType))
    )
}



