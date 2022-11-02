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

import com.arangodb.ArangoDBException
import com.arangodb.async.{ArangoDBAsync, ArangoDatabaseAsync}
import com.arangodb.velocypack.module.scala.VPackScalaModule
import org.slf4s.Logging
import org.springframework.beans.factory.DisposableBean
import za.co.absa.commons.lang.OptionImplicits.AnyWrapper
import za.co.absa.commons.version.Version
import za.co.absa.commons.version.impl.SemVer20Impl.SemanticVersion

import javax.net.ssl._
import scala.concurrent._
import scala.util.Try

class ArangoDatabaseFacade(connectionURL: ArangoConnectionURL, maybeSSLContext: Option[SSLContext]) extends DisposableBean {

  import za.co.absa.spline.persistence.ArangoDatabaseFacade._

  private val ArangoConnectionURL(_, maybeUser, maybePassword, hostsWithPorts, dbName) = connectionURL

  private val isSecure = connectionURL.isSecure

  private val arango: ArangoDBAsync = {
    val arangoBuilder = new ArangoDBAsync.Builder()
      .registerModule(new VPackScalaModule)
      .having(maybeUser)(_ user _)
      .having(maybePassword)(_ password _)

    // enable SSL if required
    if (isSecure) {
      arangoBuilder
        .useSsl(true)
        .having(maybeSSLContext)(_ sslContext _)
    }

    // Active failover mode is disabled according to https://github.com/AbsaOSS/spline/issues/1056
    // This is fixed in Spline 1.0.0, see https://github.com/AbsaOSS/spline/issues/1050
    arangoBuilder.acquireHostList(false)

    for ((host, port) <- hostsWithPorts) arangoBuilder.host(host, port)

    // build ArangoDB Client
    arangoBuilder.build
  }

  // The val is lazy to not prevent a facade instance from being created.
  // It allows connection to be re-attempted later and the {{shutdown()}} method to be called.
  lazy val db: ArangoDatabaseAsync = {
    val db = arango.db(dbName)
    warmUpDb(db)
    db
  }

  override def destroy(): Unit = {
    try {
      arango.shutdown()
    } catch {
      // this is a workaround for https://github.com/arangodb/arangodb-java-driver/issues/399
      case _: ArangoDBException =>
        // the second call works apparently because an ArangoDB resource that has thrown this exception on shutdown,
        // is already closed at this point regardless the exception. So now we're closing remaining resources.
        Try(arango.shutdown())
    }
  }
}

object ArangoDatabaseFacade extends Logging {

  import za.co.absa.commons.version.Version._

  val MinArangoVerRequired: SemanticVersion = semver"3.6.0"
  val MinArangoVerRecommended: SemanticVersion = semver"3.7.3"

  private def warmUpDb(db: ArangoDatabaseAsync): Unit = {
    val verStr = try {
      blocking {
        db.arango
          .getVersion
          .get
          .getVersion
      }
    } catch {
      case ce: ExecutionException =>
        throw ce.getCause
    }

    val arangoVer = Version.asSemVer(verStr)

    // check ArangoDb server version requirements
    if (arangoVer < MinArangoVerRequired)
      sys.error(s"" +
        s"Unsupported ArangoDB server version ${arangoVer.asString}. " +
        s"Required version: ${MinArangoVerRequired.asString} or later. " +
        s"Recommended version: ${MinArangoVerRecommended.asString} or later.")

    if (arangoVer < MinArangoVerRecommended)
      log.warn(s"WARNING: " +
        s"The ArangoDB server version ${arangoVer.asString} might contain a bug that can cause Spline malfunction. " +
        s"It's highly recommended to upgrade to ArangoDB ${MinArangoVerRecommended.asString} or later. " +
        s"See: https://github.com/arangodb/arangodb/issues/12693")
  }
}
