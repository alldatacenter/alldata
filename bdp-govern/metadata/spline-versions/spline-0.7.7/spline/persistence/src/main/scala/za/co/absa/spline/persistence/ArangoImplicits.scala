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

import com.arangodb.async.{ArangoCollectionAsync, ArangoCursorAsync, ArangoDatabaseAsync}
import com.arangodb.internal.InternalArangoDatabaseOps
import com.arangodb.model.AqlQueryOptions
import za.co.absa.spline.persistence.LogMessageUtils.createQueryLogMessage

import java.util.concurrent.CompletionException
import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters._
import scala.compat.java8.StreamConverters.RichStream
import scala.concurrent.{ExecutionContext, Future}

object ArangoImplicits {

  implicit class ArangoCollectionAsyncScalaWrapper(col: ArangoCollectionAsync)(implicit ec: ExecutionContext) {
    /**
     * The method returns `true` if the given collection exists in the database.
     * It returns `false` if the database exists, but the collection does not.
     * If the database don't exist it returns a failure.
     *
     * @return `true` if the collection exists; `false` if it doesn't exist; a failure if the database doesn't exist.
     */
    def existsCollection(): Future[Boolean] = {
      col.db.getInfo.toScala.flatMap(_ =>
        col.exists().toScala.map(Boolean.box(_)))
    }
  }

  implicit class ArangoDatabaseAsyncScalaWrapper(db: ArangoDatabaseAsync)(implicit ec: ExecutionContext)
    extends InternalArangoDatabaseOps(db) {

    def queryOne[T: Manifest](
      queryString: String,
      bindVars: Map[String, AnyRef] = Map.empty,
      options: AqlQueryOptions = null
    ): Future[T] = {
      for (
        cur <- queryAs[T](queryString, bindVars, options)
        if cur.hasNext
      ) yield cur.next
    }

    def queryStream[T: Manifest](
      queryString: String,
      bindVars: Map[String, AnyRef] = Map.empty,
      options: AqlQueryOptions = null
    ): Future[Stream[T]] = {
      queryAs[T](queryString, bindVars, options)
        .map(_.streamRemaining().toScala)
    }

    def queryOptional[T: Manifest](
      queryString: String,
      bindVars: Map[String, AnyRef] = Map.empty,
      options: AqlQueryOptions = null
    ): Future[Option[T]] = {
      queryAs[T](queryString, bindVars, options)
        .map(cur =>
          if (cur.hasNext) Some(cur.next)
          else None
        )
    }

    def queryAs[T: Manifest](queryString: String,
      bindVars: Map[String, AnyRef] = Map.empty,
      options: AqlQueryOptions = null
    ): Future[ArangoCursorAsync[T]] = {
      val resultType = implicitly[Manifest[T]].runtimeClass.asInstanceOf[Class[T]]
      db.query(queryString, bindVars.asJava, options, resultType)
        .toScala
        .recover {
          case e: CompletionException =>
            val queryMsg = createQueryLogMessage(queryString)
            throw new DatabaseException(queryMsg, e.getCause)
        }
    }
  }

}
