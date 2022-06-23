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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import za.co.absa.spline.common.webmvc.jackson.NullAcceptingDeserializer

sealed trait ExpressionLike {
  def id: ExpressionLike.Id

  // todo: data types aren't properly modeled yet. Shouldn't we hide them under extras then?
  def dataType: Option[Any]

  def childRefs: Seq[ExpressionLike.ChildRef]
  def extra: Map[String, Any]
}

object ExpressionLike {
  type Id = String
  type ChildRef = AttrOrExprRef
  type Params = Map[String, Any]
  type Extras = Map[String, Any]
}

/**
 * Represents a functional expression that computes a value based on the given input.
 *
 * @param id       expression ID
 * @param name     expression name
 * @param dataType output data type
 * @param childRefs input expression (or attribute) IDs
 * @param params   optional static expression parameters (don't confuse with input parameters)
 * @param extra    optional metadata
 */
case class FunctionalExpression(
  override val id: ExpressionLike.Id,
  override val dataType: Option[Any] = None,
  override val childRefs: Seq[ExpressionLike.ChildRef] = Nil,
  override val extra: Map[String, Any] = Map.empty,
  name: String,
  params: Map[String, Any] = Map.empty,
) extends ExpressionLike

/**
 * Literal expression
 *
 * @param id       expression ID
 * @param value    literal value
 * @param dataType value data type
 * @param extra    optional metadata
 */
case class Literal(
  override val id: ExpressionLike.Id,
  override val dataType: Option[Any] = None,
  override val extra: Map[String, Any] = Map.empty,
  @JsonDeserialize(using = classOf[NullAcceptingDeserializer])
  value: Any,
) extends ExpressionLike {
  override def childRefs: Seq[ExpressionLike.ChildRef] = Nil
}
