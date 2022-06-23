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

package za.co.absa.spline.persistence

import com.arangodb.async.ArangoDatabaseAsync
import za.co.absa.commons.version.Version
import za.co.absa.commons.version.Version._
import za.co.absa.commons.version.impl.SemVer20Impl.SemanticVersion
import za.co.absa.spline.persistence.DatabaseVersionManager._
import za.co.absa.spline.persistence.model.DBVersion.Status
import za.co.absa.spline.persistence.model.CollectionDef.DBVersion

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class DatabaseVersionManager(db: ArangoDatabaseAsync)(implicit ec: ExecutionContext) {

  import ArangoImplicits._

  def insertDbVersion(currentVersion: SemanticVersion): Future[SemanticVersion] = {
    val dbVersion = model.DBVersion(currentVersion.asString, model.DBVersion.Status.Current)
    for {
      exists <- db.collection(DBVersion.name).exists.toScala
      _ <-
        if (exists) Future.successful({})
        else db.createCollection(DBVersion.name).toScala
      _ <- db.collection(DBVersion.name)
        .insertDocument(dbVersion)
        .toScala
    } yield currentVersion
  }

  def currentVersion: Future[SemanticVersion] =
    getDBVersion(Status.Current)
      .map(_.getOrElse(BaselineVersion))

  def preparingVersion: Future[Option[SemanticVersion]] =
    getDBVersion(Status.Preparing)

  private def getDBVersion(status: model.DBVersion.Status.Type) = {
    db
      .collection(DBVersion.name)
      .existsCollection()
      .flatMap(exists =>
        if (exists) db.queryOptional[String](
          s"""
             |WITH ${DBVersion.name}
             |FOR v IN ${DBVersion.name}
             |    FILTER v.status == '$status'
             |    RETURN v.version
             |""".stripMargin)
          .map(_.map(Version.asSemVer))
        else Future.successful(None))
  }
}

object DatabaseVersionManager {
  val BaselineVersion = semver"0.4.0"

}
