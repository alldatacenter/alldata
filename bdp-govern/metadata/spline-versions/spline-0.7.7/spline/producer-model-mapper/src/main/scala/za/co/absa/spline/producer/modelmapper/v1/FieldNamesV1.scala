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

object FieldNamesV1 {

  object AgentInfoName {
    val Spline = "spline"
  }

  object EventExtraInfo {
    val DurationNs = "durationNs"
  }

  object PlanExtraInfo {
    val AppName = "appName"
    val Attributes = "attributes"
  }

  object OperationExtraInfo {
    val Name = "name"
  }

  object AttributeDef {
    val Id = "id"
    val Name = "name"
    val DataTypeId = "dataTypeId"
    val Dependencies = "dependencies"
  }

  object ExpressionDef {
    val TypeHint = "_typeHint"
    val DataTypeId = "dataTypeId"
    val Children = "children"
    val Child = "child"
    val Alias = "alias"
    val RefId = "refId"
    val Symbol = "symbol"
    val Value = "value"
    val Name = "name"
    val ExprType = "exprType"
    val Params = "params"
  }

}
