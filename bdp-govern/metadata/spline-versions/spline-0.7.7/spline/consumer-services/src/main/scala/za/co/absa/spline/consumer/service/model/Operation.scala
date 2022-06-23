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

@ApiModel(description = "Operation")
case class Operation
(
  @ApiModelProperty(value = "Operation Id")
  _id: Operation.Id,
  @ApiModelProperty(value = "Type of the operation", example = "Read / Transformation / Write")
  _type: Operation.Type,
  @ApiModelProperty(value = "Name of the operation")
  name: Option[String],
  @ApiModelProperty(value = "Properties of the operation")
  properties: Map[String, Any]
) extends Graph.Node {
  override type Id = Operation.Id

  def this() = this(null, null, null, null)
}

object Operation {
  type Id = String
  type Type = String // Read / Transformation / Write
}
