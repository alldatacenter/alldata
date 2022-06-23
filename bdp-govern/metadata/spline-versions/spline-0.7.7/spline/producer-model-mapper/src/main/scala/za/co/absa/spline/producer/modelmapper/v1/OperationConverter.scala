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

package za.co.absa.spline.producer.modelmapper.v1

import za.co.absa.commons.lang.Converter
import za.co.absa.spline.producer.model.v1_1
import za.co.absa.spline.producer.{model => v1}

class OperationConverter(
  objectConverter: ObjectConverter,
  maybeOutputConverter: Option[OperationOutputConverter],
  operationNameExtractor: v1.OperationLike => Option[v1_1.OperationLike.Name],
) extends Converter {
  override type From = v1.OperationLike
  override type To = v1_1.OperationLike

  override def convert(op1: v1.OperationLike): v1_1.OperationLike = {
    val convertedParams = op1
      .params
      .mapValues(objectConverter.convert)
      .view.force // see: https://github.com/scala/bug/issues/4776

    val maybeOutput = maybeOutputConverter.flatMap(_.convert(op1))

    val id = op1.id.toString
    val maybeName = operationNameExtractor(op1)
    val childIds = op1.childIds.map(_.toString)
    val extra = op1.extra

    op1 match {
      case wop1: v1.WriteOperation =>
        v1_1.WriteOperation(
          outputSource = wop1.outputSource,
          append = wop1.append,
          id = id,
          name = maybeName,
          childIds = childIds,
          params = convertedParams,
          extra = extra
        )
      case rop1: v1.ReadOperation =>
        v1_1.ReadOperation(
          inputSources = rop1.inputSources,
          id = id,
          name = maybeName,
          output = maybeOutput,
          params = convertedParams,
          extra = extra
        )
      case _: v1.DataOperation =>
        v1_1.DataOperation(
          id = id,
          name = maybeName,
          childIds = childIds,
          output = maybeOutput,
          params = convertedParams,
          extra = extra
        )
    }
  }
}
