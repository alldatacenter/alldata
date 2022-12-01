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

package za.co.absa.spline.producer.model.v1_1

import za.co.absa.commons.graph.GraphImplicits.DAGNodeIdMapping

sealed trait OperationLike {
  def id: OperationLike.Id
  def name: Option[OperationLike.Name]
  def childIds: Seq[OperationLike.Id]
  def output: Option[OperationLike.Schema]
  def params: Map[String, Any]
  def extra: Map[String, Any]
}

object OperationLike {
  type Id = String
  type Name = String
  type Schema = Seq[Attribute.Id]

  implicit object OpNav extends DAGNodeIdMapping[OperationLike, OperationLike.Id] {

    override def selfId(op: OperationLike): Id = op.id

    override def refIds(op: OperationLike): Seq[Id] = op.childIds
  }

}


case class DataOperation(
  override val id: OperationLike.Id,
  override val name: Option[OperationLike.Name] = None,
  override val childIds: Seq[OperationLike.Id] = Nil,
  override val output: Option[OperationLike.Schema] = None,
  override val params: Map[String, Any] = Map.empty,
  override val extra: Map[String, Any] = Map.empty
) extends OperationLike

case class ReadOperation(
  inputSources: Seq[String],
  override val id: OperationLike.Id,
  override val name: Option[OperationLike.Name] = None,
  override val output: Option[OperationLike.Schema] = None,
  override val params: Map[String, Any] = Map.empty,
  override val extra: Map[String, Any] = Map.empty
) extends OperationLike {
  override def childIds: Seq[OperationLike.Id] = Nil
}

case class WriteOperation(
  outputSource: String,
  append: Boolean,
  override val id: OperationLike.Id,
  override val name: Option[OperationLike.Name] = None,
  override val childIds: Seq[OperationLike.Id],
  override val params: Map[String, Any] = Map.empty,
  override val extra: Map[String, Any] = Map.empty
) extends OperationLike {
  override def output: Option[OperationLike.Schema] = None
}
