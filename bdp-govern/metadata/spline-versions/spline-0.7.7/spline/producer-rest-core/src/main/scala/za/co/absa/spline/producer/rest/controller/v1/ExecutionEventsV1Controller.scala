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
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import springfox.documentation.annotations.ApiIgnore
import za.co.absa.spline.producer.model.ExecutionEvent
import za.co.absa.spline.producer.modelmapper.v1.ModelMapperV1
import za.co.absa.spline.producer.service.repo.ExecutionProducerRepository

import scala.concurrent.{ExecutionContext, Future}

@ApiIgnore
@Controller
@RequestMapping(consumes = Array("application/json"))
class ExecutionEventsV1Controller @Autowired()(
  val repo: ExecutionProducerRepository) {

  import ExecutionContext.Implicits.global

  @PostMapping(Array("/execution-events"))
  @ResponseStatus(HttpStatus.CREATED)
  def executionEvent(@RequestBody eventsV1: Array[ExecutionEvent]): Future[Unit] = {
    val events = eventsV1.map(ModelMapperV1.fromDTO)
    repo.insertExecutionEvents(events)
  }

}
