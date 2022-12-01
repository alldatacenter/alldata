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

package za.co.absa.spline.harvester

import za.co.absa.spline.harvester.postprocessing.metadata.EvaluatedTemplate
import za.co.absa.spline.producer.model._

object ExtraMetadataImplicits {

  import za.co.absa.commons.lang.OptionImplicits._

  implicit class Ops[A: ExtraAdder](entity: A) {
    def withAddedExtra(moreExtra: Map[String, Any]): A =
      if (moreExtra.isEmpty) entity
      else implicitly[ExtraAdder[A]].addedExtra(entity, moreExtra)

    def withAddedMetadata(metadata: EvaluatedTemplate): A = {
      val entityWithExtra =
        if (metadata.extra.isEmpty) entity
        else implicitly[ExtraAdder[A]].addedExtra(entity, metadata.extra)

      if (metadata.labels.isEmpty) entityWithExtra
      else implicitly[ExtraAdder[A]].addedLabels(entity, metadata.labels)
    }
  }

  trait ExtraAdder[A] {
    def addedExtra(a: A, m: Map[String, Any]): A
    def addedLabels(a: A, m: Map[String, Seq[String]]): A
  }

  object ExtraAdder {

    implicit object ExecPlanExtraAdder extends ExtraAdder[ExecutionPlan] {
      override def addedExtra(a: ExecutionPlan, m: Map[String, Any]): ExecutionPlan =
        a.copy(extraInfo = (a.extraInfo.getOrElse(Map.empty) ++ m).asOption)

      override def addedLabels(a: ExecutionPlan, m: Map[String, Seq[String]]): ExecutionPlan =
        a.copy(labels = (a.labels.getOrElse(Map.empty) ++ m).asOption)
    }

    implicit object ExecEventExtraAdder extends ExtraAdder[ExecutionEvent] {
      override def addedExtra(a: ExecutionEvent, m: Map[String, Any]): ExecutionEvent =
        a.copy(extra = (a.extra.getOrElse(Map.empty) ++ m).asOption)

      override def addedLabels(a: ExecutionEvent, m: Map[String, Seq[String]]): ExecutionEvent =
        a.copy(labels = (a.labels.getOrElse(Map.empty) ++ m).asOption)
    }

    implicit object ReadOperationExtraAdder extends ExtraAdder[ReadOperation] {
      override def addedExtra(a: ReadOperation, m: Map[String, Any]): ReadOperation =
        a.copy(extra = (a.extra.getOrElse(Map.empty) ++ m).asOption)

      override def addedLabels(a: ReadOperation, m: Map[String, Seq[String]]): Nothing =
        throw new UnsupportedOperationException
    }

    implicit object WriteOperationExtraAdder extends ExtraAdder[WriteOperation] {
      override def addedExtra(a: WriteOperation, m: Map[String, Any]): WriteOperation =
        a.copy(extra = (a.extra.getOrElse(Map.empty) ++ m).asOption)

      override def addedLabels(a: WriteOperation, m: Map[String, Seq[String]]): Nothing =
        throw new UnsupportedOperationException
    }

    implicit object DataOperationExtraAdder extends ExtraAdder[DataOperation] {
      override def addedExtra(a: DataOperation, m: Map[String, Any]): DataOperation =
        a.copy(extra = (a.extra.getOrElse(Map.empty) ++ m).asOption)

      override def addedLabels(a: DataOperation, m: Map[String, Seq[String]]): Nothing =
        throw new UnsupportedOperationException
    }

  }

}
