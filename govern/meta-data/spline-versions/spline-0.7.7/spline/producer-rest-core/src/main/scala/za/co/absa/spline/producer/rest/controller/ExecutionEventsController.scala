/*
 * Copyright 2019 ABSA Group Limited
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

package za.co.absa.spline.producer.rest.controller

import io.swagger.annotations.{Api, ApiOperation, ApiResponse, ApiResponses}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import za.co.absa.spline.producer.model.v1_1.ExecutionEvent
import za.co.absa.spline.producer.rest.ProducerAPI
import za.co.absa.spline.producer.service.repo.ExecutionProducerRepository

import scala.concurrent.{ExecutionContext, Future}

@Controller
@RequestMapping(consumes = Array(ProducerAPI.MimeTypeV1_1))
@Api(tags = Array("execution"))
class ExecutionEventsController @Autowired()(
  val repo: ExecutionProducerRepository) {

  import ExecutionContext.Implicits.global

  @PostMapping(Array("/execution-events"))
  @ApiOperation(
    value = "Save Execution Events",
    notes =
      """
        Saves a list of Execution Events.

        Payload format:

        [
          {
            // Reference to the execution plan Id that was triggered
            planId: <UUID>,

            // [Optional] A label that logically distinguish a group of one of multiple execution plans from another group.
            // If set, it has to match the discriminator of the associated execution plan.
            // The property is used for UUID collision detection.
            discriminator: <string>,

            // Time (milliseconds since Epoch) when the execution finished
            timestamp: <number>,

            // [Optional] Duration (in nanoseconds) of the execution
            durationNs: <number>,
            // [Optional] Additional info about the error (in case there was an error during the execution)
            error: {...},

            // [Optional] Any other extra information related to the given execution event
            extra: {...}
          },
          ...
        ]
      """)
  @ApiResponses(Array(
    new ApiResponse(code = 201, message = "All execution Events are successfully stored")
  ))
  @ResponseStatus(HttpStatus.CREATED)
  def executionEvent(@RequestBody execEvents: Array[ExecutionEvent]): Future[Unit] = {
    repo.insertExecutionEvents(execEvents)
  }

}
