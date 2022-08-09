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

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class AutoClosingArangoManagerProxy(
  managerProvider: ArangoDatabaseAsync => ArangoManager,
  arangoFacadeProvider: () => ArangoDatabaseFacade)
  (implicit val ex: ExecutionContext)
  extends ArangoManager {

  override def initialize(onExistsAction: OnDBExistsAction): Future[Boolean] =
    withManager(_.initialize(onExistsAction))

  override def upgrade(): Future[Unit] =
    withManager(_.upgrade())

  override def execute(actions: AuxiliaryDBAction*): Future[Unit] =
    withManager(_.execute(actions: _*))

  private def withManager[A](fn: ArangoManager => Future[A]): Future[A] = {
    val dbFacade = arangoFacadeProvider()

    (Try(fn(managerProvider(dbFacade.db))) match {
      case Failure(e) => Future.failed(e)
      case Success(v) => v
    }) andThen {
      case _ => dbFacade.destroy()
    }
  }

}
