/*
 * Copyright 2021 ABSA Group Limited
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

package za.co.absa.spline.harvester.dispatcher.modelmapper

import za.co.absa.commons.version.Version
import za.co.absa.spline.harvester.dispatcher.ProducerApiVersion
import za.co.absa.spline.producer.model._

trait ModelMapper[TPlanDTO <: AnyRef, TEventDTO <: AnyRef] {

  def toDTO(plan: ExecutionPlan): Option[TPlanDTO]
  def toDTO(event: ExecutionEvent): Option[TEventDTO]
}

object ModelMapper {
  def forApiVersion(version: Version): ModelMapper[_ <: AnyRef, _ <: AnyRef] = version match {
    case ProducerApiVersion.V1_2 => ModelMapperV12
    case ProducerApiVersion.V1_1 => ModelMapperV11
    case ProducerApiVersion.V1 => ModelMapperV10
  }
}
