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

package za.co.absa.spline.persistence.model

sealed trait Operation extends Vertex {
  def name: Option[Operation.Name]
  def params: Map[String, Any]
  def extra: Map[String, Any]
  def `type`: Operation.Type
}

object Operation {
  type Name = String
  type Type = String
}

case class Read(
  inputSources: Seq[String],
  override val name: Option[Operation.Name],
  override val params: Map[String, Any],
  override val extra: Map[String, Any],
  override val _key: ArangoDocument.Key,
  override val _belongsTo: Option[ArangoDocument.Id]
) extends Operation {
  def this() = this(null, null, null, null, null, null)

  override val `type`: Operation.Type = "Read"
}

case class Write(
  outputSource: String,
  append: Boolean,
  override val name: Option[Operation.Name],
  override val params: Map[String, Any],
  override val extra: Map[String, Any],
  override val _key: ArangoDocument.Key,
  override val _belongsTo: Option[ArangoDocument.Id]
) extends Operation {
  def this() = this(null, false, null, null, null, null, null)

  override val `type`: Operation.Type = "Write"
}

case class Transformation(
  override val name: Option[Operation.Name],
  override val params: Map[String, Any],
  override val extra: Map[String, Any],
  override val _key: ArangoDocument.Key,
  override val _belongsTo: Option[ArangoDocument.Id]
) extends Operation {
  def this() = this(null, null, null, null, null)

  override val `type`: Operation.Type = "Transformation"
}
