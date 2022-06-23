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

package za.co.absa.spline.consumer.service.model

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(description = "Expression Node")
case class ExpressionNode
(
  @ApiModelProperty(value = "Expression Id")
  _id: ExpressionNode.Id,

  @ApiModelProperty(value = "Expression data type")
  dataType: Option[String],

  @ApiModelProperty(value = "Expression name")
  name: String,

  @ApiModelProperty(value = "Literal expression value")
  value: Option[Any],

  @ApiModelProperty(value = "Expression parameters")
  params: Map[String, Any],

  @ApiModelProperty(value = "Expression extras")
  extra: Map[String, Any],


) extends Graph.Node {
  def this() = this(null, null, null, null, null, null)

  override type Id = ExpressionNode.Id
}

object ExpressionNode {
  type Id = String
}
