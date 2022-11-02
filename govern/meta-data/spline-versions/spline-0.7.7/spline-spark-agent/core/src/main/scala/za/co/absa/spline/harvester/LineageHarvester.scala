/*
 * Copyright 2017 ABSA Group Limited
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

import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.spark
import org.apache.spark.internal.Logging
import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.execution.{LeafExecNode, SparkPlan}
import za.co.absa.commons.CollectionImplicits._
import za.co.absa.commons.graph.GraphImplicits._
import za.co.absa.commons.lang.CachingConverter
import za.co.absa.commons.lang.OptionImplicits._
import za.co.absa.commons.reflect.ReflectionUtils
import za.co.absa.commons.reflect.extractors.SafeTypeMatchingExtractor
import za.co.absa.spline.harvester.LineageHarvester._
import za.co.absa.spline.harvester.ModelConstants.{AppMetaInfo, ExecutionEventExtra, ExecutionPlanExtra}
import za.co.absa.spline.harvester.builder._
import za.co.absa.spline.harvester.builder.read.ReadCommandExtractor
import za.co.absa.spline.harvester.builder.write.{WriteCommand, WriteCommandExtractor}
import za.co.absa.spline.harvester.converter.DataTypeConverter
import za.co.absa.spline.harvester.iwd.IgnoredWriteDetectionStrategy
import za.co.absa.spline.harvester.logging.ObjectStructureLogging
import za.co.absa.spline.harvester.postprocessing.PostProcessor
import za.co.absa.spline.producer.model._

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

class LineageHarvester(
  ctx: HarvestingContext,
  writeCommandExtractor: WriteCommandExtractor,
  readCommandExtractor: ReadCommandExtractor,
  iwdStrategy: IgnoredWriteDetectionStrategy,
  postProcessor: PostProcessor,
  dataTypeConverter: DataTypeConverter with CachingConverter,
  opNodeBuilderFactory: OperationNodeBuilderFactory
) extends Logging with ObjectStructureLogging {

  def harvest(result: Either[Throwable, Duration]): HarvestResult = {
    logDebug(s"Harvesting lineage from ${ctx.logicalPlan.getClass}")

    val (readMetrics: Metrics, writeMetrics: Metrics) = ctx.executedPlanOpt.
      map(getExecutedReadWriteMetrics).
      getOrElse((Map.empty, Map.empty))

    tryExtractWriteCommand(ctx.logicalPlan).flatMap(writeCommand => {
      val writeOpBuilder = opNodeBuilderFactory.writeNodeBuilder(writeCommand)
      val restOpBuilders = createOperationBuildersRecursively(writeCommand.query)

      restOpBuilders.lastOption.foreach(writeOpBuilder.+=)
      val builders = restOpBuilders :+ writeOpBuilder

      val restOps = restOpBuilders.map(_.build())
      val writeOp = writeOpBuilder.build()

      val (opReads, opOthers) =
        restOps.foldLeft((Vector.empty[ReadOperation], Vector.empty[DataOperation])) {
          case ((accRead, accOther), opRead: ReadOperation) => (accRead :+ opRead, accOther)
          case ((accRead, accOther), opOther: DataOperation) => (accRead, accOther :+ opOther)
        }

      if (writeCommand.mode == SaveMode.Ignore && iwdStrategy.wasWriteIgnored(writeMetrics)) {
        logDebug("Ignored write detected. Skipping lineage.")
        None
      }
      else {
        val planWithoutId = {
          val planExtra = Map[String, Any](
            ExecutionPlanExtra.AppName -> ctx.session.sparkContext.appName,
            ExecutionPlanExtra.DataTypes -> dataTypeConverter.values
          )

          val attributes = (builders.map(_.outputAttributes) :+ writeOpBuilder.additionalAttributes)
            .reduce(_ ++ _)
            .distinct

          val expressions = Expressions(
            constants = builders.map(_.literals).reduce(_ ++ _).asOption,
            functions = builders.map(_.functionalExpressions).reduce(_ ++ _).asOption
          )

          val p = ExecutionPlan(
            id = None,
            discriminator = None,
            labels = None,
            name = ctx.session.sparkContext.appName.asOption, // `appName` for now, but could be different (user defined) in the future
            operations = Operations(writeOp, opReads.asOption, opOthers.asOption),
            attributes = attributes.asOption,
            expressions = expressions.asOption,
            systemInfo = SparkVersionInfo,
            agentInfo = SplineVersionInfo.asOption,
            extraInfo = planExtra.asOption
          )
          postProcessor.process(p)
        }

        val planId = ctx.idGenerators.execPlanIdGenerator.nextId(planWithoutId)
        val plan = planWithoutId.copy(id = Some(planId))

        val event = {
          val maybeDurationNs = result.right.toOption.map(_.toNanos)
          val maybeErrorString = result.left.toOption.map(ExceptionUtils.getStackTrace)

          val eventExtra = Map[String, Any](
            ExecutionEventExtra.AppId -> ctx.session.sparkContext.applicationId,
            ExecutionEventExtra.ReadMetrics -> readMetrics,
            ExecutionEventExtra.WriteMetrics -> writeMetrics
          )

          val ev = ExecutionEvent(
            planId = planId,
            discriminator = None,
            labels = None,
            timestamp = System.currentTimeMillis,
            durationNs = maybeDurationNs,
            error = maybeErrorString,
            extra = eventExtra.asOption)

          postProcessor.process(ev)
        }

        logDebug(s"Successfully harvested lineage from ${ctx.logicalPlan.getClass}")
        Some(plan -> event)
      }
    })
  }

  private def tryExtractWriteCommand(plan: LogicalPlan): Option[WriteCommand] =
    Try(writeCommandExtractor.asWriteCommand(plan)) match {
      case Success(Some(write)) => Some(write)
      case Success(None) =>
        logDebug(s"${plan.getClass} was not recognized as a write-command. Skipping.")
        logObjectStructureAsTrace(plan)
        None
      case Failure(e) =>
        logObjectStructureAsError(plan)
        throw new RuntimeException(s"Write extraction failed for: ${plan.getClass}", e)
    }

  private def createOperationBuildersRecursively(rootOp: LogicalPlan): Seq[OperationNodeBuilder] = {
    @scala.annotation.tailrec
    def traverseAndCollect(
      accBuilders: Seq[OperationNodeBuilder],
      processedEntries: Map[LogicalPlan, OperationNodeBuilder],
      enqueuedEntries: Seq[(LogicalPlan, OperationNodeBuilder)]
    ): Seq[OperationNodeBuilder] = {
      enqueuedEntries match {
        case Nil => accBuilders
        case (curOpNode, parentBuilder) +: restEnqueuedEntries =>
          val maybeExistingBuilder = processedEntries.get(curOpNode)
          val curBuilder = maybeExistingBuilder.getOrElse(createOperationBuilder(curOpNode))

          if (parentBuilder != null) parentBuilder += curBuilder

          if (maybeExistingBuilder.isEmpty) {

            val newNodesToProcess = extractChildren(curOpNode)

            traverseAndCollect(
              curBuilder +: accBuilders,
              processedEntries + (curOpNode -> curBuilder),
              newNodesToProcess.map(_ -> curBuilder) ++ restEnqueuedEntries)

          } else {
            traverseAndCollect(accBuilders, processedEntries, restEnqueuedEntries)
          }
      }
    }

    val builders = traverseAndCollect(Nil, Map.empty, Seq((rootOp, null)))
    builders.sortedTopologicallyBy(_.operationId, _.childIds, reverse = true)
  }

  private def createOperationBuilder(op: LogicalPlan): OperationNodeBuilder =
    readCommandExtractor.asReadCommand(op)
      .map(opNodeBuilderFactory.readNodeBuilder)
      .getOrElse(opNodeBuilderFactory.genericNodeBuilder(op))

  private def extractChildren(plan: LogicalPlan) = plan match {
    case AnalysisBarrierExtractor(_) =>
      // special handling - spark 2.3 sometimes includes AnalysisBarrier in the plan
      val child = ReflectionUtils.extractFieldValue[LogicalPlan](plan, "child")
      Seq(child)

    case _ => plan.children
  }
}

object LineageHarvester {

  import za.co.absa.commons.version.Version

  val SparkVersionInfo: NameAndVersion = NameAndVersion(
    name = AppMetaInfo.Spark,
    version = spark.SPARK_VERSION
  )

  val SplineVersionInfo: NameAndVersion = NameAndVersion(
    name = AppMetaInfo.Spline,
    version = {
      val splineSemver = Version.asSemVer(SplineBuildInfo.Version)
      if (splineSemver.preRelease.isEmpty) SplineBuildInfo.Version
      else s"${SplineBuildInfo.Version}+${SplineBuildInfo.Revision}"
    }
  )

  type Metrics = Map[String, Long]
  private type HarvestResult = Option[(ExecutionPlan, ExecutionEvent)]

  private def getExecutedReadWriteMetrics(executedPlan: SparkPlan): (Metrics, Metrics) = {
    def getNodeMetrics(node: SparkPlan): Metrics = node.metrics.mapValues(_.value)

    val cumulatedReadMetrics: Metrics = {
      @scala.annotation.tailrec
      def traverseAndCollect(acc: Metrics, nodes: Seq[SparkPlan]): Metrics = {
        nodes match {
          case Nil => acc
          case (leaf: LeafExecNode) +: queue =>
            traverseAndCollect(acc |+| getNodeMetrics(leaf), queue)
          case (node: SparkPlan) +: queue =>
            traverseAndCollect(acc, node.children ++ queue)
        }
      }

      traverseAndCollect(Map.empty, Seq(executedPlan))
    }

    (cumulatedReadMetrics, getNodeMetrics(executedPlan))
  }

  object AnalysisBarrierExtractor extends SafeTypeMatchingExtractor[LogicalPlan](
    "org.apache.spark.sql.catalyst.plans.logical.AnalysisBarrier")

}
