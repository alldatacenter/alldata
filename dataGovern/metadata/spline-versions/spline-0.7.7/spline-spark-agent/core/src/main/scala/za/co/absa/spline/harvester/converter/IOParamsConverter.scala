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

import org.apache.spark.sql.catalyst.TableIdentifier
import org.apache.spark.sql.catalyst.expressions.Expression
import za.co.absa.commons.lang.Converter

class IOParamsConverter(
  exprToRefConverter: ExprToRefConverter
) extends Converter {
  override type From = Map[String, _]
  override type To = Map[String, _]

  private def valueDecomposer = ValueDecomposer.addHandler(_ => {
    case (exp: Expression, _) => Some(exprToRefConverter.convert(exp))
    case (ti: TableIdentifier, _) => Some(Map("table" -> ti.table, "database" -> ti.database))
  })

  override def convert(operation: Map[String, _]): Map[String, _] =
    valueDecomposer.decompose(operation, "notUsed")
      .getOrElse(Map.empty)
      .asInstanceOf[Map[String, _]]
}
