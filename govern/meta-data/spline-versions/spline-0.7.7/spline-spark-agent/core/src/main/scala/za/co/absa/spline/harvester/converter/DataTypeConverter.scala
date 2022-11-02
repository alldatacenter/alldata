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

import org.apache.spark.sql.{types => st}
import za.co.absa.commons.lang.Converter
import za.co.absa.spline.harvester.IdGenerator
import za.co.absa.spline.model.dt._

import java.util.UUID

class DataTypeConverter(idGen: IdGenerator[Any, UUID]) extends Converter {
  override type From = (st.DataType, Boolean)
  override type To = DataType

  override def convert(arg: From): DataType = {
    val (sparkDataType, nullable) = arg
    sparkDataType match {
      case structType: st.StructType =>
        Struct(idGen.nextId(), structType.fields.map(field =>
          StructField(field.name, convert(field.dataType -> field.nullable).id)), nullable)

      case arrayType: st.ArrayType =>
        Array(idGen.nextId(), convert(arrayType.elementType -> arrayType.containsNull).id, nullable)

      case otherType: st.DataType =>
        Simple(idGen.nextId(), otherType.simpleString, nullable)
    }
  }

  final def convert(sparkDataType: st.DataType, nullable: Boolean): DataType = convert(sparkDataType -> nullable)
}
