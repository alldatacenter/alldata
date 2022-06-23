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

package za.co.absa.spline.consumer.rest.controller

import io.swagger.annotations._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation._
import za.co.absa.spline.consumer.rest.controller.LineageDetailedController.AttributeLineageAndImpact
import za.co.absa.spline.consumer.service.model.{AttributeGraph, DataSourceActionType, ExecutionPlanInfo, LineageDetailed}
import za.co.absa.spline.consumer.service.repo.{DataSourceRepository, ExecutionPlanRepository}

import scala.concurrent.{ExecutionContext, Future}

@RestController
@Api(tags = Array("lineage"))
class LineageDetailedController @Autowired()(
  val epRepo: ExecutionPlanRepository,
  val dsRepo: DataSourceRepository,
) {

  import ExecutionContext.Implicits.global

  @GetMapping(Array("lineage-detailed"))
  @ApiOperation(
    value = "Get detailed execution plan (DAG)",
    notes = "Returns a logical plan DAG by execution plan ID")
  def lineageDetailed(
    @ApiParam(value = "Execution plan ID")
    @RequestParam("execId") execId: ExecutionPlanInfo.Id
  ): Future[LineageDetailed] = {
    epRepo.findById(execId)
  }

  @GetMapping(Array("attribute-lineage-and-impact"))
  @ApiOperation(
    value = "Get graph of attributes that depends on attribute with provided id")
  def attributeLineageAndImpact(
    @ApiParam(value = "Attribute ID")
    @RequestParam("attributeId") attributeId: String
  ): Future[AttributeLineageAndImpact] =
    Future.sequence(Seq(
      epRepo.execPlanAttributeLineage(attributeId),
      epRepo.execPlanAttributeImpact(attributeId),
    )).map({
      case Seq(lin, imp) => AttributeLineageAndImpact(Some(lin), imp)
    })

  @GetMapping(value = Array("execution-plans/{plan_id}/data-sources"))
  @ResponseStatus(HttpStatus.OK)
  def execPlanDataSources(
    @PathVariable("plan_id") planId: ExecutionPlanInfo.Id,
    @ApiParam(value = "access")
    @RequestParam(name = "access", required = false) access: String
  ): Future[Array[String]] = {
    val dataSourceActionTypeOption = DataSourceActionType.findValueOf(access)
    dsRepo.findByUsage(planId, dataSourceActionTypeOption)
  }
}

object LineageDetailedController {

  @ApiModel(description = "Attribute Lineage And Impact")
  case class AttributeLineageAndImpact(
    @ApiModelProperty("Attribute Lineage")
    lineage: Option[AttributeGraph],
    @ApiModelProperty("Attribute Impact")
    impact: AttributeGraph
  )

}
