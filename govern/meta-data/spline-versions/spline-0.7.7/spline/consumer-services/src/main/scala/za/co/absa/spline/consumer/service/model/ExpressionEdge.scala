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
import za.co.absa.spline.consumer.service.model.ExpressionEdge.ExpressionEdgeType
import za.co.absa.spline.persistence.model.Edge

@ApiModel(description = "`Uses` or `Takes` edge")
case class ExpressionEdge
(
  @ApiModelProperty(value = "Source Node")
  source: ExpressionNode.Id,

  @ApiModelProperty(value = "Target Node")
  target: ExpressionNode.Id,

  @ApiModelProperty(value = "Edge type ('uses' or 'takes')")
  `type`: ExpressionEdgeType,

  @ApiModelProperty(value = "JSONPath of inside 'source' entity where this edge is logically attached to (if applicable)")
  path: Edge.FromPath,

  @ApiModelProperty(value = "0-based order (if applicable)")
  index: Int
) extends Graph.Edge {
  def this() = this(null, null, null, null, -1)

  override type JointId = ExpressionNode.Id
}

object ExpressionEdge {

  type ExpressionEdgeType = String

  object ExpressionEdgeType {
    val Takes = "takes"
    val Uses = "uses"
  }

}
