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

import za.co.absa.commons.reflect.EnumerationMacros

sealed abstract class DataSourceActionType(val name: String)

object DataSourceActionType {

  case object Read extends DataSourceActionType("read")

  case object Write extends DataSourceActionType("write")


  val values: Set[DataSourceActionType] = EnumerationMacros.sealedInstancesOf[DataSourceActionType]

  def findValueOf(name: String): Option[DataSourceActionType] =
    DataSourceActionType.values.find(_.name.equalsIgnoreCase(name))
}
