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

package za.co.absa.spline.harvester.postprocessing

import za.co.absa.spline.harvester.HarvestingContext
import za.co.absa.spline.producer.model._

class PostProcessor(filters: Seq[PostProcessingFilter], ctx: HarvestingContext) {

  private def provideCtx[E](f: (E, HarvestingContext) => E): E => E =
    (e: E) => f(e, ctx)

  private def chainFilters[E](f: PostProcessingFilter => (E, HarvestingContext) => E): E => E =
    filters
      .map(f(_))
      .map(provideCtx)
      .reduceOption(_.andThen(_))
      .getOrElse(identity)

  def process(event: ExecutionEvent): ExecutionEvent =
    chainFilters(_.processExecutionEvent)(event)

  def process(plan: ExecutionPlan): ExecutionPlan =
    chainFilters(_.processExecutionPlan)(plan)

  def process(op: ReadOperation): ReadOperation =
    chainFilters(_.processReadOperation)(op)

  def process(op: WriteOperation): WriteOperation =
    chainFilters(_.processWriteOperation)(op)

  def process(op: DataOperation): DataOperation =
    chainFilters(_.processDataOperation)(op)
}
