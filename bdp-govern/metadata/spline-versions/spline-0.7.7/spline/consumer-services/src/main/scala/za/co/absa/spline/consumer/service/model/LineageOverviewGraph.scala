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

package za.co.absa.spline.consumer.service.model

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(description = "Lineage")
case class LineageOverviewGraph
(
  @ApiModelProperty(value = "Array of Lineage Overview nodes representing an Execution or a DataSource")
  nodes: Array[LineageOverviewNode],
  @ApiModelProperty(value = "Link between the Executions and the DataSources")
  edges: Array[Transition],
  @ApiModelProperty(value = "Requested max depth")
  depthRequested: Int,
  @ApiModelProperty(value = "Computed depth")
  depthComputed: Int,
) extends Graph {
  def this() = this(null, null, -1, -1)

  override type Node = LineageOverviewNode
  override type Edge = Transition
}
