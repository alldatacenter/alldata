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

package za.co.absa.spline.persistence

import com.arangodb.async.ArangoDatabaseAsync
import com.arangodb.entity.{EdgeDefinition, IndexType}
import com.arangodb.model._
import org.apache.commons.io.FilenameUtils
import org.slf4s.Logging
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import za.co.absa.commons.lang.ARM
import za.co.absa.commons.reflect.EnumerationMacros.sealedInstancesOf
import za.co.absa.commons.version.impl.SemVer20Impl.SemanticVersion
import za.co.absa.spline.persistence.OnDBExistsAction.{Drop, Skip}
import za.co.absa.spline.persistence.foxx.{FoxxManager, FoxxSourceResolver}
import za.co.absa.spline.persistence.migration.Migrator
import za.co.absa.spline.persistence.model.{CollectionDef, GraphDef, ViewDef}

import scala.collection.JavaConverters._
import scala.collection.immutable._
import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

trait ArangoManager {

  /**
   * @return `true` if actual initialization was performed.
   */
  def initialize(onExistsAction: OnDBExistsAction): Future[Boolean]
  def upgrade(): Future[Unit]
  def execute(actions: AuxiliaryDBAction*): Future[Unit]
}

class ArangoManagerImpl(
  db: ArangoDatabaseAsync,
  dbVersionManager: DatabaseVersionManager,
  migrator: Migrator,
  foxxManager: FoxxManager,
  appDBVersion: SemanticVersion)
  (implicit val ex: ExecutionContext)
  extends ArangoManager
    with Logging {

  import ArangoManagerImpl._

  def initialize(onExistsAction: OnDBExistsAction): Future[Boolean] = {
    log.debug("Initialize database")
    db.exists.toScala.flatMap { exists =>
      if (exists && onExistsAction == Skip) {
        log.debug("Database already exists - skipping initialization")
        Future.successful(false)
      } else for {
        _ <- deleteDbIfRequested(db, onExistsAction == Drop)
        _ <- db.create().toScala
        _ <- createCollections(db)
        _ <- createAQLUserFunctions(db)
        _ <- createFoxxServices()
        _ <- createIndices(db)
        _ <- createGraphs(db)
        _ <- createViews(db)
        _ <- dbVersionManager.insertDbVersion(appDBVersion)
      } yield true
    }
  }

  override def upgrade(): Future[Unit] = {
    log.debug("Upgrade database")
    dbVersionManager.currentVersion
      .flatMap(currentVersion => {
        log.info(s"Current database version: ${currentVersion.asString}")
        log.info(s"Target database version: ${appDBVersion.asString}")
        if (currentVersion == appDBVersion) Future.successful {
          log.info(s"The database is up-to-date")
        } else if (currentVersion > appDBVersion) Future.failed {
          new RuntimeException("Database downgrade is not supported")
        } else for {
          _ <- deleteFoxxServices()
          _ <- migrator.migrate(currentVersion, appDBVersion)
          _ <- createFoxxServices()
        } yield {}
      })
  }

  override def execute(actions: AuxiliaryDBAction*): Future[Unit] = {
    actions.foldLeft(Future.successful(())) {
      case (prevFuture, nextAction) =>
        prevFuture.flatMap(_ => (nextAction match {
          case AuxiliaryDBAction.CheckDBAccess => checkDBAccess(db)
          case AuxiliaryDBAction.FoxxReinstall => reinstallFoxxServices()
          case AuxiliaryDBAction.IndicesDelete => deleteIndices(db)
          case AuxiliaryDBAction.IndicesCreate => createIndices(db)
          case AuxiliaryDBAction.ViewsDelete => deleteViews(db)
          case AuxiliaryDBAction.ViewsCreate => createViews(db)
        }).map(_ => {}))
    }
  }

  private def checkDBAccess(db: ArangoDatabaseAsync) = {
    db.exists.toScala
  }

  private def reinstallFoxxServices() = {
    for {
      _ <- deleteFoxxServices()
      _ <- createFoxxServices()
    } yield {}
  }

  private def deleteDbIfRequested(db: ArangoDatabaseAsync, dropIfExists: Boolean) = {
    for {
      exists <- db.exists.toScala
      _ <- if (exists && !dropIfExists)
        throw new IllegalArgumentException(s"Arango Database ${db.name()} already exists")
      else if (exists && dropIfExists) {
        log.debug("Drop existing database")
        db.drop().toScala
      }
      else Future.successful(Unit)
    } yield Unit
  }

  private def createCollections(db: ArangoDatabaseAsync) = {
    log.debug(s"Create collections")
    Future.sequence(
      for (colDef <- sealedInstancesOf[CollectionDef])
        yield db.createCollection(colDef.name, new CollectionCreateOptions().`type`(colDef.collectionType)).toScala)
  }

  private def createGraphs(db: ArangoDatabaseAsync) = {
    log.debug(s"Create graphs")
    Future.sequence(
      for (graphDef <- sealedInstancesOf[GraphDef]) yield {
        val edgeDefs = graphDef.edgeDefs.map(e =>
          (new EdgeDefinition)
            .collection(e.name)
            .from(e.froms.map(_.name): _*)
            .to(e.tos.map(_.name): _*))
        db.createGraph(graphDef.name, edgeDefs.asJava).toScala
      })
  }

  private def deleteIndices(db: ArangoDatabaseAsync) = {
    for {
      cols <- db.getCollections.toScala.map(_.asScala.filter(!_.getIsSystem))
      eventualIndices = cols.map(col => db.collection(col.getName).getIndexes.toScala.map(_.asScala))
      allIndices <- Future.reduceLeft(Iterable(eventualIndices.toSeq: _*))(_ ++ _)
      userIndices = allIndices.filter(idx => idx.getType != IndexType.primary && idx.getType != IndexType.edge)
      _ <- Future.traverse(userIndices)(idx => db.deleteIndex(idx.getId).toScala)
    } yield {}
  }

  private def createIndices(db: ArangoDatabaseAsync) = {
    log.debug(s"Create indices")
    Future.sequence(
      for {
        colDef <- sealedInstancesOf[CollectionDef]
        idxDef <- colDef.indexDefs
      } yield {
        val dbCol = db.collection(colDef.name)
        val fields = idxDef.fields.asJava
        (idxDef.options match {
          case opts: FulltextIndexOptions => dbCol.ensureFulltextIndex(fields, opts)
          case opts: GeoIndexOptions => dbCol.ensureGeoIndex(fields, opts)
          case opts: PersistentIndexOptions => dbCol.ensurePersistentIndex(fields, opts)
          case opts: TtlIndexOptions => dbCol.ensureTtlIndex(fields, opts)
        }).toScala
      })
  }

  private def createAQLUserFunctions(db: ArangoDatabaseAsync) = {
    log.debug(s"Lookup AQL functions to register")
    Future.sequence(
      new PathMatchingResourcePatternResolver(getClass.getClassLoader)
        .getResources(s"$AQLFunctionsLocation/*.js").toSeq
        .map(res => {
          val functionName = s"$AQLFunctionsPrefix::${FilenameUtils.removeExtension(res.getFilename).toUpperCase}"
          val functionBody = ARM.using(Source.fromURL(res.getURL))(_.getLines.mkString("\n"))
          log.debug(s"Register AQL function: $functionName")
          log.trace(s"AQL function body: $functionBody")
          db.createAqlFunction(functionName, functionBody, new AqlFunctionCreateOptions()).toScala
        }))
  }

  private def createFoxxServices(): Future[_] = {
    log.debug(s"Lookup Foxx services to install")
    val serviceDefs = FoxxSourceResolver.lookupSources(FoxxSourcesLocation)
    log.debug(s"Found Foxx services: ${serviceDefs.map(_._1) mkString ", "}")
    Future.traverse(serviceDefs.toSeq) {
      case (name, assets) => foxxManager.install(s"/$name", assets)
    }
  }

  private def deleteFoxxServices(): Future[_] = {
    log.debug(s"Delete Foxx services")
    foxxManager.list().flatMap(srvDefs =>
      Future.sequence(for {
        srvDef <- srvDefs
        srvMount = srvDef("mount").toString if !srvMount.startsWith("/_")
      } yield foxxManager.uninstall(srvMount)
      ).map(_ => {})
    )
  }

  private def deleteViews(db: ArangoDatabaseAsync) = {
    Future.traverse(sealedInstancesOf[ViewDef])(viewDef => {
      val view = db.view(viewDef.name)
      view.exists().toScala.flatMap { exists =>
        if (exists)
          view.drop().toScala
        else
          Future.successful(())
      }
    })
  }

  private def createViews(db: ArangoDatabaseAsync) = {
    log.debug(s"Create views")
    Future.traverse(sealedInstancesOf[ViewDef]) { viewDef =>
      for {
        _ <- db.createArangoSearch(viewDef.name, null).toScala
        _ <- db.arangoSearch(viewDef.name).updateProperties(viewDef.properties).toScala
      } yield Unit
    }
  }
}

object ArangoManagerImpl {
  private val AQLFunctionsLocation = "classpath:AQLFunctions"
  private val AQLFunctionsPrefix = "SPLINE"
  private val FoxxSourcesLocation = "classpath:foxx"
}
