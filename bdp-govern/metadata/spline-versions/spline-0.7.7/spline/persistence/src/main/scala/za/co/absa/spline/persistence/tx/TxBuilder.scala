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

package za.co.absa.spline.persistence.tx

import java.util.UUID

import com.arangodb.async.ArangoDatabaseAsync
import com.arangodb.model.TransactionOptions
import za.co.absa.spline.persistence.model.{ArangoDocument, CollectionDef}
import za.co.absa.spline.persistence.tx.TxBuilder.{ArangoTxImpl, condLine}

import scala.compat.java8.FutureConverters._
import scala.concurrent.Future

sealed trait Query {
  def collectionDefs: Seq[CollectionDef]
}

case class NativeQuery(
  query: String,
  params: Map[String, Any] = Map.empty,
  override val collectionDefs: Seq[CollectionDef] = Nil
) extends Query

case class UpdateQuery(
  collectionDef: CollectionDef,
  filter: String,
  data: Map[String, Any]
) extends Query {
  override def collectionDefs: Seq[CollectionDef] = Seq(collectionDef)
}

object UpdateQuery {
  val DocWildcard = s"_${UUID.randomUUID}_"
}

case class InsertQuery(
  collectionDef: CollectionDef,
  documents: Seq[ArangoDocument],
  ignoreExisting: Boolean = false
) extends Query {
  override def collectionDefs: Seq[CollectionDef] = Seq(collectionDef)
}

object InsertQuery {
  def apply(colDef: CollectionDef, docs: ArangoDocument*): InsertQuery = InsertQuery(colDef, docs)
}

class TxBuilder {

  private var queries: Seq[Query] = Vector.empty

  def addQuery(q: Query): this.type = {
    queries :+= q
    this
  }

  def buildTx: ArangoTx = new ArangoTxImpl(this)

  private[tx] def generateJs(): String = {
    val statements = queries.zipWithIndex.map {
      case (nq: NativeQuery, i) =>
        s"""
           |(function(db, params){
           |  ${nq.query}
           |})(_db, _params[$i]);
           |""".stripMargin.trim

      case (iq: InsertQuery, i) =>
        val colName = iq.collectionDef.name
        Seq(
          s"_params[$i].forEach(o =>",
          condLine(iq.ignoreExisting,
            s"""
               |  o._key && _db._collection("$colName").exists(o._key) ||
               |  o._from && o._to && _db._query(`
               |    WITH $colName
               |    FOR e IN $colName
               |        FILTER e._from == @o._from && e._to == @o._to
               |        LIMIT 1
               |        COLLECT WITH COUNT INTO cnt
               |        RETURN !!cnt
               |    `, {o}).next() ||
               |  """.stripMargin),
          s"""_db._collection("$colName").insert(o, {silent:true}));"""
        ).mkString

      case (uq: UpdateQuery, i) =>
        val colName = uq.collectionDef.name
        val doc = "a"
        val filter = uq.filter.replace(UpdateQuery.DocWildcard, doc)
        s"""
           |_db._query(`
           |  WITH $colName
           |  FOR $doc IN $colName
           |      FILTER $filter
           |      UPDATE $doc._key WITH @b IN $colName
           |`, {"b": _params[$i]});
           |""".stripMargin.trim
    }
    s"""
       |function (_params) {
       |  const _db = require('internal').db;
       |  ${statements.mkString("\n").replace("\n", "\n  ")}
       |}
       |""".stripMargin
  }

  private def options: TransactionOptions = {
    val params = queries
      .map({
        case nq: NativeQuery => nq.params
        case iq: InsertQuery => iq.documents.toVector
        case uq: UpdateQuery => uq.data
      })
    val writeCollections = queries
      .flatMap(_.collectionDefs)
      .map(_.name)
      .distinct
    new TransactionOptions()
      .params(params)
      .writeCollections(writeCollections: _*)
      .allowImplicit(false)
  }
}

object TxBuilder {

  private class ArangoTxImpl(txBuilder: TxBuilder) extends ArangoTx {
    override def execute(db: ArangoDatabaseAsync): Future[Unit] =
      db.transaction(txBuilder.generateJs(), classOf[Unit], txBuilder.options).toScala
  }

  private def condLine(cond: => Boolean, stmt: => String): String = if (cond) stmt else ""
}
