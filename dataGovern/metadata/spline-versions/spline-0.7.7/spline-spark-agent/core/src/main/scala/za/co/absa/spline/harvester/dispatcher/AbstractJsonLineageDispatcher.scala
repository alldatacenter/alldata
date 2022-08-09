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

package za.co.absa.spline.harvester.dispatcher

import za.co.absa.commons.EnumUtils.EnumOps
import za.co.absa.commons.reflect.EnumerationMacros
import za.co.absa.spline.harvester.dispatcher.AbstractJsonLineageDispatcher.ModelEntity
import za.co.absa.spline.producer.model.{ExecutionEvent, ExecutionPlan}

import za.co.absa.spline.harvester.json.HarvesterJsonSerDe

abstract class AbstractJsonLineageDispatcher
  extends LineageDispatcher {

  import ModelEntity.Implicits._
  import HarvesterJsonSerDe.impl._

  final override def send(plan: ExecutionPlan): Unit = send(Array(getEntityName(ModelEntity.Plan), plan).toJson)

  final override def send(event: ExecutionEvent): Unit = send(Array(getEntityName(ModelEntity.Event), event).toJson)

  protected def send(json: String): Unit

  private def getEntityName(entity: ModelEntity): String = entity.name.toLowerCase
}

object AbstractJsonLineageDispatcher {

  sealed trait ModelEntity

  object ModelEntity extends EnumOps[ModelEntity] {

    object Plan extends ModelEntity

    object Event extends ModelEntity

    protected val values: Seq[ModelEntity] = EnumerationMacros.sealedInstancesOf[ModelEntity].toSeq
  }

}
