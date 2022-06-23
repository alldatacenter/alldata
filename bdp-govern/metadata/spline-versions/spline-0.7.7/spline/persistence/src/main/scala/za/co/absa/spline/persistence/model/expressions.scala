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

sealed trait Expression extends Vertex {
  def `type`: String
  def dataType: Option[Any]
  def extra: Map[String, Any]
}

case class FunctionalExpression(
  override val _key: ArangoDocument.Key,
  override val _belongsTo: Option[ArangoDocument.Id],
  override val dataType: Option[Any],
  override val extra: Map[String, Any],
  name: String,
  arity: Int,
  params: Map[String, Any],
) extends Expression {
  def this() = this(null, null, null, null, null, -1, null)

  val `type`: String = "Func"
}

case class LiteralExpression(
  override val _key: ArangoDocument.Key,
  override val _belongsTo: Option[ArangoDocument.Id],
  override val dataType: Option[Any],
  override val extra: Map[String, Any],
  value: Any,
) extends Expression {
  def this() = this(null, null, null, null, null)

  val `type`: String = "Lit"
}
