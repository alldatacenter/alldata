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
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertions, OneInstancePerTest}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import za.co.absa.commons.version.impl.SemVer20Impl.SemanticVersion
import za.co.absa.spline.persistence.ArangoManagerImplSpec._
import za.co.absa.spline.persistence.foxx.FoxxManager
import za.co.absa.spline.persistence.migration.Migrator

import scala.concurrent.{ExecutionContext, Future}

class ArangoManagerImplSpec
  extends AsyncFlatSpec
    with OneInstancePerTest
    with MockitoSugar
    with Matchers {

  import za.co.absa.commons.version.Version._

  behavior of "upgrade()"

  it should "call foxx manager and migrator properly in order" in {
    val dbVerMgrMock = mock[DatabaseVersionManager]
    val migratorMock = mock[Migrator]
    val foxxMgrMock = mock[FoxxManager]

    when(dbVerMgrMock.currentVersion)
      .thenReturn(Future.successful(semver"1.2.3"))

    when(foxxMgrMock.install(any(), any()))
      .thenReturn(Future.successful({}))

    when(foxxMgrMock.uninstall(any()))
      .thenReturn(Future.successful({}))

    when(foxxMgrMock.list())
      .thenReturn(Future.successful(Seq(
        Map("mount" -> "aaa"),
        Map("mount" -> "bbb"))))

    when(migratorMock.migrate(any(), any()))
      .thenReturn(Future.successful(true))

    val manager = newManager(
      migratorMock = migratorMock,
      dbVersionManagerMock = dbVerMgrMock,
      foxxManagerMock = foxxMgrMock,
      appDbVersion = semver"4.5.6")

    for {
      _ <- manager.upgrade()
    } yield {
      val inOrder = Mockito.inOrder(foxxMgrMock, migratorMock)
      inOrder verify foxxMgrMock list()
      inOrder verify foxxMgrMock uninstall "aaa"
      inOrder verify foxxMgrMock uninstall "bbb"
      inOrder verify migratorMock migrate(semver"1.2.3", semver"4.5.6")
      inOrder verify foxxMgrMock install(ArgumentMatchers.eq("/spline"), ArgumentMatchers.any())
      Assertions.succeed
    }
  }

  it should "fail when foxx services failed to install" in {
    val dbVerMgrMock = mock[DatabaseVersionManager]
    val migratorMock = mock[Migrator]
    val foxxMgrMock = mock[FoxxManager]

    when(dbVerMgrMock.currentVersion)
      .thenReturn(Future.successful(semver"1.2.3"))

    when(foxxMgrMock.install(any(), any()))
      .thenReturn(Future.failed(new RuntimeException("oops")))

    when(foxxMgrMock.list())
      .thenReturn(Future.successful(Seq.empty))

    when(migratorMock.migrate(any(), any()))
      .thenReturn(Future.successful(true))

    val manager = newManager(
      migratorMock = migratorMock,
      dbVersionManagerMock = dbVerMgrMock,
      foxxManagerMock = foxxMgrMock,
      appDbVersion = semver"4.5.6")

    for {
      ex <- manager.upgrade().failed
    } yield {
      ex.getMessage shouldEqual "oops"
    }
  }
}

object ArangoManagerImplSpec {
  private def newManager(
    migratorMock: Migrator = null,
    foxxManagerMock: FoxxManager = null,
    dbVersionManagerMock: DatabaseVersionManager = null,
    appDbVersion: SemanticVersion = null
  )(implicit ec: ExecutionContext): ArangoManagerImpl = {
    new ArangoManagerImpl(
      mock[ArangoDatabaseAsync],
      dbVersionManagerMock,
      migratorMock,
      foxxManagerMock,
      appDbVersion)
  }
}
