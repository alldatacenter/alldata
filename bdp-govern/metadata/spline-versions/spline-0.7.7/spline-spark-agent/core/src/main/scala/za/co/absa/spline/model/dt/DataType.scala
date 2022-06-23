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

package za.co.absa.spline.model.dt

import java.util.UUID

/**
 * The trait describes a data type of an attribute, expression, etc.
 */
sealed trait DataType {
  val id: UUID
  val nullable: Boolean

  def childDataTypeIds: Seq[UUID]
}

case class Simple(id: UUID, name: String, nullable: Boolean) extends DataType {
  override def childDataTypeIds: Seq[UUID] = Nil
}

case class Struct(id: UUID, fields: Seq[StructField], nullable: Boolean) extends DataType {
  override def childDataTypeIds: Seq[UUID] = fields.map(_.dataTypeId)
}

case class StructField(name: String, dataTypeId: UUID)

case class Array(id: UUID, elementDataTypeId: UUID, nullable: Boolean) extends DataType {
  override def childDataTypeIds: Seq[UUID] = Seq(elementDataTypeId)
}
