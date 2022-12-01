/*
 * Copyright 2022 ABSA Group Limited
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

package za.co.absa.spline.harvester.dispatcher.modelmapper

import za.co.absa.commons.lang.OptionImplicits._
import za.co.absa.commons.version.Version
import za.co.absa.spline.harvester.LineageHarvester
import za.co.absa.spline.harvester.dispatcher.modelmapper.OpenLineageModelMapper._
import za.co.absa.spline.harvester.dispatcher.openlineage.model.facet.SplinePayloadFacet
import za.co.absa.spline.producer.model.openlineage.v0_3_1._
import za.co.absa.spline.producer.model.{ExecutionEvent, ExecutionPlan}

import java.time.{Duration, Instant}
import java.util.UUID

class OpenLineageModelMapper(splineModelMapper: ModelMapper[_, _], apiVersion: Version, namespace: String) {

  def toDtos(plan: ExecutionPlan, event: ExecutionEvent): Seq[RunEvent] = {
    val runId = UUID.randomUUID()
    val job = Job(namespace = namespace, name = plan.name.getOrElse("no name"), facets = None)

    val completeTime = Instant.ofEpochMilli(event.timestamp)
    val duration = Duration.ofNanos(event.durationNs.getOrElse(0))
    val startTime = completeTime.minus(duration)

    val eventStart = RunEvent(
      eventType = EventType.Start.asOption,
      eventTime = java.util.Date.from(startTime),
      run = Run(runId = runId, facets = None),
      job = job,
      inputs = None,
      outputs = None,
      producer = Producer,
      schemaURL = SchemaUrl
    )

    val eventCompleted = RunEvent(
      eventType = event.error.map(_ => EventType.Fail).orElse(EventType.Complete.asOption),
      eventTime = java.util.Date.from(completeTime),
      run = Run(runId = runId, facets = Some(Map(
        SplineEvent -> createSplinePayloadFacet(splineModelMapper.toDTO(event), "TODO"), // see issue #416
        SplinePlan -> createSplinePayloadFacet(splineModelMapper.toDTO(plan), "TODO") // see issue #416
      ))),
      job = job,
      inputs = plan.operations.reads
        .getOrElse(Seq.empty)
        .flatMap(ro => ro.inputSources.map(createInputDataset))
        .asOption,
      outputs = Some(Seq(createOutputDataset(plan.operations.write.outputSource))),
      producer = Producer,
      schemaURL = SchemaUrl
    )

    Seq(eventStart, eventCompleted)
  }

  private def createSplinePayloadFacet(payload: AnyRef, payloadSchemaUrl: String) =
    new SplinePayloadFacet(
      _producer = Producer,
      _schemaURL = "TODO", // see issue #416
      payloadSchemaURL = payloadSchemaUrl,
      payload = payload
    )

  private def createInputDataset(source: String): InputDataset = {
    val (namespace, name) = OpenLineageUriMapper.uriToNamespaceAndName(source)
    InputDataset(
      namespace = namespace,
      name = name,
      facets = None,
      inputFacets = None
    )
  }

  private def createOutputDataset(source: String): OutputDataset = {
    val (namespace, name) = OpenLineageUriMapper.uriToNamespaceAndName(source)
    OutputDataset(
      namespace = namespace,
      name = name,
      facets = None,
      outputFacets = None
    )
  }

}

object OpenLineageModelMapper {
  private val Producer = s"https://github.com/AbsaOSS/spline-spark-agent/tree/release/${LineageHarvester.SplineVersionInfo.version}"
  private val SchemaUrl = "https://openlineage.io/spec/1-0-2/OpenLineage.json#/$defs/RunEvent"

  object EventType {
    val Start = "START"
    val Complete = "COMPLETE"
    val Fail = "FAIL"
  }

  private val SplineEvent = "splineEvent"
  private val SplinePlan = "splineEvent"
}
