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

package za.co.absa.spline.producer.rest.controller.v1

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation._
import springfox.documentation.annotations.ApiIgnore
import za.co.absa.spline.producer.model.ExecutionPlan
import za.co.absa.spline.producer.modelmapper.v1.ModelMapperV1
import za.co.absa.spline.producer.service.InconsistentEntityException
import za.co.absa.spline.producer.service.repo.ExecutionProducerRepository

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@ApiIgnore
@RestController
@RequestMapping(consumes = Array("application/json"))
class ExecutionPlansV1Controller @Autowired()(
  val repo: ExecutionProducerRepository) {

  import ExecutionContext.Implicits.global

  @PostMapping(Array("/execution-plans"))
  @ResponseStatus(HttpStatus.CREATED)
  def executionPlan(@RequestBody planV1: ExecutionPlan): Future[UUID] = {
    val plan =
      try {
        ModelMapperV1.fromDTO(planV1)
      } catch {
        case NonFatal(e) => throw new InconsistentEntityException(e.getMessage)
      }
    repo
      .insertExecutionPlan(plan)
      .map(_ => plan.id)
  }
}
