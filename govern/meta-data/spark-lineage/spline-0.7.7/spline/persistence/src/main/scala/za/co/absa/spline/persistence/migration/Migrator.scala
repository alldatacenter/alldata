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

package za.co.absa.spline.persistence.migration

import com.arangodb.async.ArangoDatabaseAsync
import org.slf4s.Logging
import za.co.absa.commons.version.impl.SemVer20Impl.SemanticVersion
import za.co.absa.spline.persistence.model.DBVersion.Status
import za.co.absa.spline.persistence.model.CollectionDef.DBVersion
import za.co.absa.spline.persistence.tx._
import za.co.absa.spline.persistence.{ArangoImplicits, DatabaseVersionManager, model}

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class Migrator(
  db: ArangoDatabaseAsync,
  scriptRepository: MigrationScriptRepository,
  dbVersionManager: DatabaseVersionManager)
  (implicit ec: ExecutionContext) extends Logging {

  def migrate(verFrom: SemanticVersion, verTo: SemanticVersion): Future[Boolean] = {
    val eventualMigrationChain =
      for {
        dbVersionExists <- db.collection(DBVersion.name).exists.toScala
        maybePreparingVersion <- dbVersionManager.preparingVersion
        _ <-
          if (dbVersionExists) Future.successful({})
          else dbVersionManager.insertDbVersion(DatabaseVersionManager.BaselineVersion)
      } yield {
        maybePreparingVersion.foreach(prepVersion =>
          sys.error("" +
            s"Incomplete upgrade to version: ${prepVersion.asString} detected." +
            " The previous DB upgrade has probably failed" +
            " or another application is performing it at the moment." +
            " The database might be left in an inconsistent state." +
            " Please restore the database backup before proceeding," +
            " or wait until the ongoing upgrade has finished.")
        )
        val migrationChain = scriptRepository.findMigrationChain(verFrom, verTo)
        migrationChain
      }

    eventualMigrationChain.flatMap(migrationChain => {
      if (migrationChain.nonEmpty) {
        log.info(s"The database is ${migrationChain.length} versions behind. Migration will be performed.")
        log.debug(s"Migration scripts to apply: $migrationChain")
      }
      migrationChain
        .foldLeft(Future.successful({})) {
          case (prevMigrationEvidence, scr) => prevMigrationEvidence.flatMap(_ => {
            log.debug(s"Applying script: $scr")
            executeMigration(scr.script, scr.verTo)
          })
        }
        .map(_ => migrationChain.nonEmpty)
    })
  }

  private def executeMigration(script: String, version: SemanticVersion): Future[Unit] = {
    log.info(s"Upgrading to version: ${version.asString}")
    log.trace(s"Applying script: \n$script")

    import ArangoImplicits._

    for {
      _ <- db.adminExecute(
        // It also serves as a pre-flight check of the '/_admin/execute' API
        s"console.log('Starting migration to version ${version.asString}')"
      )
      _ <- db.collection(DBVersion.name).insertDocument(model.DBVersion(version.asString, Status.Preparing)).toScala
      _ <- db.adminExecute(script)
      _ <- new TxBuilder()
        .addQuery(UpdateQuery(DBVersion,
          s"${UpdateQuery.DocWildcard}.status == '${Status.Current}'", Map("status" -> Status.Upgraded.toString)))
        .addQuery(UpdateQuery(DBVersion,
          s"${UpdateQuery.DocWildcard}.status == '${Status.Preparing}'", Map("status" -> Status.Current.toString)))
        .buildTx
        .execute(db)
    } yield ()
  }
}
