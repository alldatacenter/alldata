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

@ApiModel(description = "Information Details of an Operation containing the input and output schemas with the description of the dataTypes")
case class OperationDetails
(
  operation: Operation,
  @ApiModelProperty(value = "Array of the used DataTypes in the schemas")
  dataTypes: Array[DataType],
  @ApiModelProperty(value = "Array of all the schemas")
  schemas: Array[Array[Attribute]],
  @ApiModelProperty(value = "Array of indexes of the schemas Array. The schemas at these indexes represent the input schemas")
  inputs: Array[Integer],
  @ApiModelProperty(value = "Index of the schemas Array. The schema at this index represents the output schemas")
  output: Integer

) {
  def this() = this(null, null, null, null, null)
}
