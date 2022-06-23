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

package za.co.absa.spline.producer.service.repo

import com.arangodb.async.ArangoDatabaseAsync
import org.slf4s.Logging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import za.co.absa.spline.common.AsyncCallRetryer
import za.co.absa.spline.persistence.model._
import za.co.absa.spline.persistence.tx.{ArangoTx, InsertQuery, TxBuilder}
import za.co.absa.spline.persistence.ArangoImplicits
import za.co.absa.spline.producer.model.v1_1.ExecutionEvent._
import za.co.absa.spline.producer.model.{v1_1 => apiModel}
import za.co.absa.spline.producer.service.model.{ExecutionEventKeyCreator, ExecutionPlanPersistentModel, ExecutionPlanPersistentModelBuilder}
import za.co.absa.spline.producer.service.{InconsistentEntityException, UUIDCollisionDetectedException}

import java.util.UUID
import scala.compat.java8.FutureConverters._
import scala.compat.java8.StreamConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Repository
class ExecutionProducerRepositoryImpl @Autowired()(db: ArangoDatabaseAsync, repeater: AsyncCallRetryer) extends ExecutionProducerRepository
  with Logging {

  import ArangoImplicits._
  import ExecutionProducerRepositoryImpl._

  override def insertExecutionPlan(executionPlan: apiModel.ExecutionPlan)(implicit ec: ExecutionContext): Future[Unit] = repeater.execute({
    // Here I have to use the type parameter `Any` and cast to `String` later due to ArangoDb Java driver issue.
    // See https://github.com/arangodb/arangodb-java-driver/issues/389
    val eventualMaybeExistingDiscriminatorOpt: Future[Option[String]] = db.queryOptional[Any](
      s"""
         |WITH ${NodeDef.ExecutionPlan.name}
         |FOR ex IN ${NodeDef.ExecutionPlan.name}
         |    FILTER ex._key == @key
         |    LIMIT 1
         |    RETURN ex.discriminator
         |    """.stripMargin,
      Map("key" -> executionPlan.id)
    ).map(_.map(Option(_).map(_.toString).orNull))

    val eventualPersistedDSKeyByURI: Future[Map[DataSource.Uri, DataSource.Key]] = db.queryAs[DataSource](
      s"""
         |WITH ${NodeDef.DataSource.name}
         |FOR ds IN ${NodeDef.DataSource.name}
         |    FILTER ds.uri IN @refURIs
         |    RETURN ds
         |    """.stripMargin,
      Map("refURIs" -> executionPlan.dataSources.toArray)
    ).map(_.streamRemaining.toScala.map(ds => ds.uri -> ds._key).toMap)

    for {
      persistedDSKeyByURI <- eventualPersistedDSKeyByURI
      maybeExistingDiscriminatorOpt <- eventualMaybeExistingDiscriminatorOpt
      _ <- maybeExistingDiscriminatorOpt match {
        case Some(existingDiscriminatorOrNull) =>
          // execution plan with the given ID already exists
          ensureNoExecPlanIDCollision(executionPlan.id, executionPlan.discriminator.orNull, existingDiscriminatorOrNull)
          Future.successful(Unit)
        case None =>
          // no execution plan with the given ID found
          createInsertTransaction(executionPlan, persistedDSKeyByURI).execute(db)
      }
    } yield Unit
  })

  override def insertExecutionEvents(events: Array[apiModel.ExecutionEvent])(implicit ec: ExecutionContext): Future[Unit] = repeater.execute({
    val eventualExecPlanInfos: Future[Seq[ExecPlanInfo]] = db.queryStream[ExecPlanInfo](
      s"""
         |WITH executionPlan, executes, operation, dataSource
         |FOR ep IN executionPlan
         |    FILTER ep._key IN @keys
         |
         |    LET wo = FIRST(FOR v IN 1 OUTBOUND ep executes RETURN v)
         |    LET ds = FIRST(FOR v IN 1 OUTBOUND ep affects RETURN v)
         |
         |    RETURN {
         |        key           : ep._key,
         |        discriminator : ep.discriminator,
         |        details: {
         |            "executionPlanKey" : ep._key,
         |            "frameworkName"    : CONCAT(ep.systemInfo.name, " ", ep.systemInfo.version),
         |            "applicationName"  : ep.name,
         |            "dataSourceUri"    : ds.uri,
         |            "dataSourceName"   : ds.name,
         |            "dataSourceType"   : wo.extra.destinationType,
         |            "append"           : wo.append
         |        }
         |    }
         |""".stripMargin,
      Map("keys" -> events.map(_.planId))
    )

    for {
      execPlansInfos <- eventualExecPlanInfos
      (execPlanDiscrById, execPlansDetails) = execPlansInfos
        .foldLeft((Map.empty[apiModel.ExecutionPlan.Id, apiModel.ExecutionPlan.Discriminator], Vector.empty[ExecPlanDetails])) {
          case ((descrByIdAcc, detailsAcc), ExecPlanInfo(id, discr, details)) =>
            (descrByIdAcc + (UUID.fromString(id) -> discr), detailsAcc :+ details)
        }
      res <- {
        events.foreach(e => ensureNoExecPlanIDCollision(e.planId, e.discriminator.orNull, execPlanDiscrById(e.planId)))
        createInsertTransaction(events, execPlansDetails.toArray).execute(db)
      }
    } yield res
  })

  override def isDatabaseOk()(implicit ec: ExecutionContext): Future[Boolean] = {
    try {
      val anySplineCollectionName = NodeDef.ExecutionPlan.name
      val futureIsDbOk = db.collection(anySplineCollectionName).exists.toScala.mapTo[Boolean]
      futureIsDbOk.foreach { isDbOk =>
        if (!isDbOk)
          log.error(s"Collection '$anySplineCollectionName' does not exist. Spline database is not initialized properly!")
      }
      futureIsDbOk.recover { case _ => false }
    } catch {
      case NonFatal(_) => Future.successful(false)
    }
  }
}

object ExecutionProducerRepositoryImpl {

  case class ExecPlanInfo(
    key: ArangoDocument.Key,
    discriminator: ExecutionPlan.Discriminator,
    details: ExecPlanDetails) {
    def this() = this(null, null, null)
  }

  private def createInsertTransaction(
    executionPlan: apiModel.ExecutionPlan,
    persistedDSKeyByURI: Map[DataSource.Uri, DataSource.Key]
  ) = {
    val eppm: ExecutionPlanPersistentModel =
      ExecutionPlanPersistentModelBuilder.toPersistentModel(executionPlan, persistedDSKeyByURI)

    new TxBuilder()
      // execution plan
      .addQuery(InsertQuery(NodeDef.ExecutionPlan, eppm.executionPlan))
      .addQuery(InsertQuery(EdgeDef.Executes, eppm.executes))
      .addQuery(InsertQuery(EdgeDef.Depends, eppm.depends))
      .addQuery(InsertQuery(EdgeDef.Affects, eppm.affects))

      // operation
      .addQuery(InsertQuery(NodeDef.Operation, eppm.operations))
      .addQuery(InsertQuery(EdgeDef.Follows, eppm.follows))
      .addQuery(InsertQuery(EdgeDef.ReadsFrom, eppm.readsFrom))
      .addQuery(InsertQuery(EdgeDef.WritesTo, eppm.writesTo))
      .addQuery(InsertQuery(EdgeDef.Emits, eppm.emits))
      .addQuery(InsertQuery(EdgeDef.Uses, eppm.uses))
      .addQuery(InsertQuery(EdgeDef.Produces, eppm.produces))

      // data source
      .addQuery(InsertQuery(NodeDef.DataSource, eppm.dataSources))

      // schema
      .addQuery(InsertQuery(NodeDef.Schema, eppm.schemas))
      .addQuery(InsertQuery(EdgeDef.ConsistsOf, eppm.consistsOf))

      // attribute
      .addQuery(InsertQuery(NodeDef.Attribute, eppm.attributes))
      .addQuery(InsertQuery(EdgeDef.ComputedBy, eppm.computedBy))
      .addQuery(InsertQuery(EdgeDef.DerivesFrom, eppm.derivesFrom))

      // expression
      .addQuery(InsertQuery(NodeDef.Expression, eppm.expressions))
      .addQuery(InsertQuery(EdgeDef.Takes, eppm.takes))

      .buildTx
  }

  private def createInsertTransaction(
    events: Array[apiModel.ExecutionEvent],
    execPlansDetails: Array[ExecPlanDetails]
  ): ArangoTx = {
    val referredPlanIds = events.iterator.map(_.planId).toSet
    if (referredPlanIds.size != execPlansDetails.length) {
      val existingIds = execPlansDetails.map(pd => UUID.fromString(pd.executionPlanKey))
      val missingIds = referredPlanIds -- existingIds
      throw new InconsistentEntityException(
        s"Unresolved execution plan IDs: ${missingIds mkString ", "}")
    }

    val progressNodes = events
      .zip(execPlansDetails)
      .map { case (e, pd) =>
        val key = new ExecutionEventKeyCreator(e).executionEventKey
        Progress(
          timestamp = e.timestamp,
          durationNs = e.durationNs,
          discriminator = e.discriminator,
          error = e.error,
          extra = e.extra,
          _key = key,
          execPlanDetails = pd
        )
      }

    val progressEdges = progressNodes
      .zip(events)
      .map { case (p, e) => EdgeDef.ProgressOf.edge(p._key, e.planId) }

    new TxBuilder()
      .addQuery(InsertQuery(NodeDef.Progress, progressNodes: _*).copy(ignoreExisting = true))
      .addQuery(InsertQuery(EdgeDef.ProgressOf, progressEdges: _*).copy(ignoreExisting = true))
      .buildTx
  }

  private def ensureNoExecPlanIDCollision(
    planId: apiModel.ExecutionPlan.Id,
    actualDiscriminator: apiModel.ExecutionPlan.Discriminator,
    expectedDiscriminator: apiModel.ExecutionPlan.Discriminator
  ): Unit = {
    if (actualDiscriminator != expectedDiscriminator) {
      throw new UUIDCollisionDetectedException("ExecutionPlan", planId, actualDiscriminator)
    }
  }
}
