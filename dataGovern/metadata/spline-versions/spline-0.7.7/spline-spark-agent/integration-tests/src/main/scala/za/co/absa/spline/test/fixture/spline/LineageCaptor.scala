/*
 * Copyright 2019 ABSA Group Limited
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

package za.co.absa.spline.test.fixture.spline

import org.apache.spark.sql.SparkSession
import za.co.absa.spline.agent.AgentConfig
import za.co.absa.spline.harvester.SparkLineageInitializer._
import za.co.absa.spline.producer.model.{ExecutionEvent, ExecutionPlan}

import scala.concurrent.{ExecutionContext, Future, Promise}


class LineageCaptor(builderCustomizer: AgentConfig.Builder => AgentConfig.Builder = identity)(implicit session: SparkSession) {

  @volatile private var promisedPlan = Promise[ExecutionPlan]
  @volatile private var promisedEvent = Promise[ExecutionEvent]

  session.enableLineageTracking(
    builderCustomizer(
      AgentConfig.builder()
        .lineageDispatcher(
          new LineageCapturingDispatcher(
            new LineageCaptor.Setter {
              override def capture(plan: ExecutionPlan): Unit = promisedPlan.success(plan)

              override def capture(event: ExecutionEvent): Unit = promisedEvent.success(event)
            }
          )
        )
    ).build()
  )

  def lineageOf(action: => Unit)(implicit ec: ExecutionContext): Future[LineageCaptor.CapturedLineage] = {
    action
    for {
      plan <- promisedPlan.future
      event <- promisedEvent.future
    } yield {
      promisedPlan = Promise[ExecutionPlan]
      promisedEvent = Promise[ExecutionEvent]
      plan -> Seq(event)
    }
  }
}

object LineageCaptor {

  type CapturedLineage = (ExecutionPlan, Seq[ExecutionEvent])

  trait Setter {
    def capture(plan: ExecutionPlan): Unit
    def capture(event: ExecutionEvent): Unit
  }

  trait Getter {
    def lineageOf(action: => Unit): CapturedLineage
  }
}
