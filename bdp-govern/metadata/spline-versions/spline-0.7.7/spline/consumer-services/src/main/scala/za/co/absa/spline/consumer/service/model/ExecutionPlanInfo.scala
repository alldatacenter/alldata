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

import java.util.UUID

import io.swagger.annotations.{ApiModel, ApiModelProperty}
import za.co.absa.spline.consumer.service.model.ExecutionPlanInfo.Id

@ApiModel(description = "Execution plan information")
case class ExecutionPlanInfo
(
  @ApiModelProperty(value = "Execution plan Id")
  _id: Id,
  @ApiModelProperty(value = "Name of the execution plan (script / application / job)")
  name: Option[String],
  @ApiModelProperty(value = "Name and version of the system or framework that created this execution plan")
  systemInfo: Map[String, Any],
  @ApiModelProperty(value = "Name and version of the Spline agent that collected this execution plan")
  agentInfo: Map[String, Any],
  @ApiModelProperty(value = "Other extra info")
  extra: Map[String, Any],
  @ApiModelProperty(value = "List of all input sources referenced by the read operations in the execution plan")
  inputs: Array[DataSourceInfo],
  @ApiModelProperty(value = "Write destination")
  output: DataSourceInfo
) {
  def this() = this(null, null, null, null, null, null, null)
}

object ExecutionPlanInfo {
  type Id = UUID
}
