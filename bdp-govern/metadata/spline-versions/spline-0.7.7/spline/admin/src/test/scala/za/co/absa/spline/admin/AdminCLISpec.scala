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

package za.co.absa.spline.admin

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.OneInstancePerTest
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.commons.scalatest.{ConsoleStubs, SystemExitFixture}
import za.co.absa.spline.common.SplineBuildInfo
import za.co.absa.spline.common.security.TLSUtils
import za.co.absa.spline.persistence.OnDBExistsAction.{Drop, Fail, Skip}
import za.co.absa.spline.persistence.{ArangoConnectionURL, ArangoManager, ArangoManagerFactory, OnDBExistsAction}

import javax.net.ssl.SSLContext
import scala.concurrent.Future

class AdminCLISpec
  extends AnyFlatSpec
    with OneInstancePerTest
    with MockitoSugar
    with Matchers
    with SystemExitFixture.SuiteHook
    with SystemExitFixture.Methods
    with ConsoleStubs {

  private val arangoManagerFactoryMock = mock[ArangoManagerFactory]
  private val arangoManagerMock = mock[ArangoManager]
  private val cli = new AdminCLI(arangoManagerFactoryMock)


  behavior of "AdminCLI"

  {
    it should "when called with no args, print welcome message" in {
      val msg = captureStdErr(captureExitStatus(cli.exec(Array.empty)) should be(1))
      msg should include("Try --help for more information")
    }

    for (arg <- Seq("-v", "--version"))
      it should s"when called with $arg, print the version info" in {
        val msg = captureStdOut(captureExitStatus(cli.exec(Array(arg))) should be(0))
        msg should include(SplineBuildInfo.Version)
        msg should include(SplineBuildInfo.Revision)
      }
  }


  behavior of "DB Commands"

  {
    val connUrlCaptor: ArgumentCaptor[ArangoConnectionURL] = ArgumentCaptor.forClass(classOf[ArangoConnectionURL])
    val actionFlgCaptor: ArgumentCaptor[OnDBExistsAction] = ArgumentCaptor.forClass(classOf[OnDBExistsAction])
    val sslCtxCaptor: ArgumentCaptor[Option[SSLContext]] = ArgumentCaptor.forClass(classOf[Option[SSLContext]])

    (when(
      arangoManagerFactoryMock.create(connUrlCaptor.capture, sslCtxCaptor.capture))
      thenReturn arangoManagerMock)

    (when(
      arangoManagerMock.initialize(actionFlgCaptor.capture))
      thenReturn Future.successful(true))

    (when(
      arangoManagerMock.upgrade())
      thenReturn Future.successful({}))

    (when(
      arangoManagerMock.execute(any()))
      thenReturn Future.successful({}))

    it should "when called with wrong options, print welcome message" in {
      captureStdErr {
        captureExitStatus(cli.exec(Array("db-init"))) should be(1)
      } should include("--help")

      captureStdErr {
        captureExitStatus(cli.exec(Array("db-upgrade", "-f"))) should be(1)
      } should include("--help")
    }

    it should "when called with option -k, create a non-validating SSLContext" in {
      cli.exec(Array("db-exec", "arangodbs://foo/bar", "-k"))
      sslCtxCaptor.getValue.nonEmpty should be(true)
      sslCtxCaptor.getValue.get should be(TLSUtils.TrustingAllSSLContext)
    }

    it should "when called without option -k, do not create any custom SSLContext (leave default one)" in {
      cli.exec(Array("db-exec", "arangodbs://foo/bar"))
      sslCtxCaptor.getValue.isEmpty should be(true)
    }

    behavior of "DB-Init"

    it should "initialize database" in assertingStdOut(include("DONE")) {
      cli.exec(Array("db-init", "arangodb://foo/bar"))
      connUrlCaptor.getValue should be(ArangoConnectionURL("arangodb://foo/bar"))
      actionFlgCaptor.getValue should be(Fail)
    }

    it should "initialize database eagerly" in assertingStdOut(include("DONE")) {
      cli.exec(Array("db-init", "arangodb://foo/bar", "-f"))
      connUrlCaptor.getValue should be(ArangoConnectionURL("arangodb://foo/bar"))
      actionFlgCaptor.getValue should be(Drop)
    }

    it should "initialize database lazily" in assertingStdOut(include("DONE")) {
      cli.exec(Array("db-init", "arangodb://foo/bar", "-s"))
      connUrlCaptor.getValue should be(ArangoConnectionURL("arangodb://foo/bar"))
      actionFlgCaptor.getValue should be(Skip)
    }

    it should "not allow for -s and -f to be use simultaneously" in {
      assertingStdErr(include("--force") and include("--skip") and include("cannot be used together")) {
        captureExitStatus(cli.exec(Array("db-init", "arangodb://foo/bar", "-s", "-f"))) should be(1)
      }
    }

    behavior of "DB-Upgrade"

    it should "upgrade database" in assertingStdOut(include("DONE")) {
      cli.exec(Array("db-upgrade", "arangodb://foo/bar"))
      connUrlCaptor.getValue should be(ArangoConnectionURL("arangodb://foo/bar"))
    }

    it must "not say DONE when it's not done" in {
      when(arangoManagerMock.upgrade()) thenReturn Future.failed(new Exception("Boom!"))
      assertingStdOut(not(include("DONE"))) {
        intercept[Exception] {
          cli.exec(Array("db-upgrade", "arangodb://foo/bar"))
        }
      }
    }

    behavior of "DB-exec"

    it should "call no action" in {
      cli.exec(Array("db-exec", "arangodb://foo/bar"))
      verify(arangoManagerMock).execute()
    }

    it should "call DB Manager actions in order" in {
      cli.exec(Array(
        "db-exec",
        "arangodb://foo/bar",
        "--indices-delete",
        "--views-delete",
        "--foxx-reinstall",
        "--views-create",
        "--indices-create"))

      import za.co.absa.spline.persistence.AuxiliaryDBAction._
      verify(arangoManagerMock).execute(IndicesDelete, ViewsDelete, FoxxReinstall, ViewsCreate, IndicesCreate)
    }
  }
}
