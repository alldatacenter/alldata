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

import io.swagger.annotations.ApiModelProperty
import za.co.absa.spline.persistence.model.Progress

case class WriteEventInfo
(
  @ApiModelProperty(value = "Id of the execution event")
  executionEventId: WriteEventInfo.Id,
  @ApiModelProperty(value = "Id of the execution plan")
  executionPlanId: ExecutionPlanInfo.Id,
  @ApiModelProperty(value = "Name of the framework that triggered this execution event")
  frameworkName: String,
  @ApiModelProperty(value = "Name of the application/job")
  applicationName: String,
  @ApiModelProperty(value = "Id of the application/job")
  applicationId: String,
  @ApiModelProperty(value = "When the execution was triggered")
  timestamp: WriteEventInfo.Timestamp,
  @ApiModelProperty(value = "When the execution was triggered")
  durationNs: WriteEventInfo.DurationNs,
  @ApiModelProperty(value = "Output data source name")
  dataSourceName: String,
  @ApiModelProperty(value = "Output data source URI")
  dataSourceUri: String,
  @ApiModelProperty(value = "Output data source (or data) type")
  dataSourceType: String,
  @ApiModelProperty(value = "Write mode - (true=Append; false=Override)")
  append: WriteEventInfo.Append
) {
  def this() = this(null, null, null, null, null, null, null, null, null, null, null)
}

object WriteEventInfo {
  type Id = String
  type Timestamp = java.lang.Long
  type DurationNs = Option[Progress.JobDurationInNanos]
  type Append = java.lang.Boolean
}

