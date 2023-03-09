/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.griffin.measure.sink

import scala.concurrent.Future

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame
import org.mongodb.scala._
import org.mongodb.scala.model.{Filters, UpdateOptions, Updates}
import org.mongodb.scala.result.UpdateResult

import org.apache.griffin.measure.utils.ParamUtil._
import org.apache.griffin.measure.utils.TimeUtil

/**
 * sink metric and record to mongo
 */
case class MongoSink(config: Map[String, Any], jobName: String, timeStamp: Long, block: Boolean)
    extends Sink {

  MongoConnection.init(config)

  val OverTime = "over.time"
  val Retry = "retry"

  val overTime: Long = TimeUtil.milliseconds(config.getString(OverTime, "")).getOrElse(-1L)
  val retry: Int = config.getInt(Retry, 10)

  val _MetricName = "metricName"
  val _Timestamp = "timestamp"
  val _Value = "value"

  def validate(): Boolean = MongoConnection.dataConf.available

  override def sinkRecords(records: RDD[String], name: String): Unit = {}
  override def sinkRecords(records: Iterable[String], name: String): Unit = {}

  override def sinkMetrics(metrics: Map[String, Any]): Unit = {
    mongoInsert(metrics)
  }

  private val filter =
    Filters.and(Filters.eq(_MetricName, jobName), Filters.eq(_Timestamp, timeStamp))

  private def mongoInsert(dataMap: Map[String, Any]): Unit = {
    try {
      val update = Updates.set(_Value, dataMap)
      def func(): (Long, Future[UpdateResult]) = {
        (
          timeStamp,
          MongoConnection.getDataCollection
            .updateOne(filter, update, UpdateOptions().upsert(true))
            .toFuture)
      }
      if (block) SinkTaskRunner.addBlockTask(func _, retry, overTime)
      else SinkTaskRunner.addNonBlockTask(func _, retry)
    } catch {
      case e: Throwable => error(e.getMessage, e)
    }
  }

  override def sinkBatchRecords(dataset: DataFrame, key: Option[String] = None): Unit = {}
}

object MongoConnection {

  case class MongoConf(url: String, database: String, collection: String) {
    def available: Boolean = url.nonEmpty && database.nonEmpty && collection.nonEmpty
  }

  val _MongoHead = "mongodb://"

  val Url = "url"
  val Database = "database"
  val Collection = "collection"

  private var initialed = false

  var dataConf: MongoConf = _
  private var dataCollection: MongoCollection[Document] = _

  def getDataCollection: MongoCollection[Document] = dataCollection

  def init(config: Map[String, Any]): Unit = {
    if (!initialed) {
      dataConf = mongoConf(config)
      dataCollection = mongoCollection(dataConf)
      initialed = true
    }
  }

  private def mongoConf(cfg: Map[String, Any]): MongoConf = {
    val url = cfg.getString(Url, "").trim
    val mongoUrl =
      if (url.startsWith(_MongoHead)) url
      else {
        _MongoHead + url
      }
    MongoConf(mongoUrl, cfg.getString(Database, ""), cfg.getString(Collection, ""))
  }
  private def mongoCollection(mongoConf: MongoConf): MongoCollection[Document] = {
    val mongoClient: MongoClient = MongoClient(mongoConf.url)
    val database: MongoDatabase = mongoClient.getDatabase(mongoConf.database)
    database.getCollection(mongoConf.collection)
  }

}
