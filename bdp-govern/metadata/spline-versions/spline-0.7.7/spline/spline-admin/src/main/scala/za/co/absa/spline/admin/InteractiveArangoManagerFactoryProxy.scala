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

package za.co.absa.spline.admin

import com.arangodb.ArangoDBException
import za.co.absa.spline.persistence.AuxiliaryDBAction.CheckDBAccess
import za.co.absa.spline.persistence.{ArangoConnectionURL, ArangoManager, ArangoManagerFactory}

import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import javax.net.ssl.SSLContext
import scala.PartialFunction.cond
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionException}

class InteractiveArangoManagerFactoryProxy(dbManagerFactory: ArangoManagerFactory, interactor: UserInteractor) extends ArangoManagerFactory {

  override def create(connUrl: ArangoConnectionURL, maybeSSLContext: Option[SSLContext]): ArangoManager = {
    try {
      val dbm = dbManagerFactory.create(connUrl, maybeSSLContext)
      Await.result(dbm.execute(CheckDBAccess), Duration.Inf)
      dbm
    } catch {
      case e: ArangoDBException if cond(e.getCause) {
        case ee: ExecutionException if cond(ee.getCause) {
          case ae: ArangoDBException => ae.getErrorNum == HTTP_UNAUTHORIZED
        } => connUrl.user.isEmpty || connUrl.password.isEmpty
      } =>
        val updatedConnUrl = interactor.credentializeConnectionUrl(connUrl)
        dbManagerFactory.create(updatedConnUrl, maybeSSLContext)
    }
  }
}
