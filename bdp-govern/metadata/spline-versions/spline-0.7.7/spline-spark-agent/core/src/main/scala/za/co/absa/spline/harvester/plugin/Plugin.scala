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

package za.co.absa.spline.harvester.plugin

import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import za.co.absa.spline.harvester.builder.SourceIdentifier

trait Plugin

object Plugin {
  type Params = Map[String, Any]
  type ReadNodeInfo = (SourceIdentifier, Params)
  type WriteNodeInfo = (SourceIdentifier, SaveMode, LogicalPlan, Params)

  object Precedence {
    final val Highest = 0
    final val User = 1000
    final val Normal = 5000
    final val Lowest = Int.MaxValue
  }

}
