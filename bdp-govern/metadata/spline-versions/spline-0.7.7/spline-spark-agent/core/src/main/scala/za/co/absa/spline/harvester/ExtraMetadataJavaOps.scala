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

package za.co.absa.spline.harvester

import za.co.absa.spline.producer.model._

import scala.collection.JavaConverters._

trait ExtraMetadataJavaOps {

  private type JavaMap = java.util.Map[String, AnyRef]

  import za.co.absa.spline.harvester.ExtraMetadataImplicits._

  protected def withAddedExtra(ev: ExecutionEvent, moreExtra: JavaMap): ExecutionEvent = ev.withAddedExtra(moreExtra.asScala.toMap)
  protected def withAddedExtra(pl: ExecutionPlan, moreExtra: JavaMap): ExecutionPlan = pl.withAddedExtra(moreExtra.asScala.toMap)
  protected def withAddedExtra(op: ReadOperation, moreExtra: JavaMap): ReadOperation = op.withAddedExtra(moreExtra.asScala.toMap)
  protected def withAddedExtra(op: WriteOperation, moreExtra: JavaMap): WriteOperation = op.withAddedExtra(moreExtra.asScala.toMap)
  protected def withAddedExtra(op: DataOperation, moreExtra: JavaMap): DataOperation = op.withAddedExtra(moreExtra.asScala.toMap)
}
