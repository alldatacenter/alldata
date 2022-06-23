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

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.util.{ArrayData, MapData}
import org.apache.spark.sql.types.{ArrayType, DataType, MapType, StructType}
import za.co.absa.commons.lang.Converter
import za.co.absa.commons.lang.OptionImplicits._

class DataConverter
  extends Converter {

  override type From = (Any, DataType)
  override type To = Any

  private val renderer = ValueDecomposer.addHandler(recursion => {
    case (row: InternalRow, rowType: StructType) =>
      val rowItems = row.toSeq(rowType)
      assert(rowItems.length == rowType.length)
      rowItems
        .zip(rowType)
        .map({ case (item, field) => recursion(item, field.dataType).orNull })
        .asOption

    case (md: MapData, MapType(keyType, valueType, _)) =>
      val keys = md
        .keyArray
        .toArray[Any](keyType)
        .map(_.toString)
      val values = md
        .valueArray
        .toArray[Any](valueType)
        .map(recursion(_, valueType).orNull)
      keys
        .zip(values)
        .toMap
        .asOption

    case (ad: ArrayData, ArrayType(elemType, _)) =>
      ad
        .toArray[Any](elemType)
        .map(elem => {
          val maybeValue = recursion(elem, elemType)
          maybeValue.orNull
        })
        .toSeq
        .asOption
  })

  override def convert(internalDataWithType: From): To = internalDataWithType match {
    case (v, t) => renderer.decompose(v, t).orNull
  }
}



