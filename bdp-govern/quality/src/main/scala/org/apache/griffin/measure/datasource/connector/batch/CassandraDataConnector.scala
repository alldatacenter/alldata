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

package org.apache.griffin.measure.datasource.connector.batch

import org.apache.spark.sql.{DataFrame, DataFrameReader, SparkSession}

import org.apache.griffin.measure.configuration.dqdefinition.DataConnectorParam
import org.apache.griffin.measure.context.TimeRange
import org.apache.griffin.measure.datasource.TimestampStorage
import org.apache.griffin.measure.utils.ParamUtil._

case class CassandraDataConnector(
    @transient sparkSession: SparkSession,
    dcParam: DataConnectorParam,
    timestampStorage: TimestampStorage)
    extends BatchDataConnector {

  val config: Map[String, Any] = dcParam.getConfig

  val Database = "database"
  val TableName = "table.name"
  val Where = "where"
  val Host = "host"
  val Port = "port"
  val User = "user"
  val Password = "password"

  val database: String = config.getString(Database, "default")
  val tableName: String = config.getString(TableName, "")
  val whereString: String = config.getString(Where, "")

  val host: String = config.getString(Host, "localhost")
  val port: Int = config.getInt(Port, 9042)
  val user: String = config.getString(User, "")
  val password: String = config.getString(Password, "")
  val wheres: Array[String] = whereString.split(",").map(_.trim).filter(_.nonEmpty)

  override def data(ms: Long): (Option[DataFrame], TimeRange) = {

    val dfOpt =
      try {
        sparkSession.conf.set("spark.cassandra.connection.host", host)
        sparkSession.conf.set("spark.cassandra.connection.port", port)
        sparkSession.conf.set("spark.cassandra.auth.username", user)
        sparkSession.conf.set("spark.cassandra.auth.password", password)

        val tableDef: DataFrameReader = sparkSession.read
          .format("org.apache.spark.sql.cassandra")
          .options(Map("table" -> tableName, "keyspace" -> database))

        val dataWh: String = dataWhere()

        var data: DataFrame = null
        if (wheres.length > 0) {
          data = tableDef.load().where(dataWh)
        } else {
          data = tableDef.load()
        }

        val dfOpt = Some(data)
        val preDfOpt = preProcess(dfOpt, ms)
        preDfOpt
      } catch {
        case e: Throwable =>
          error(s"load cassandra table $database.$TableName fails: ${e.getMessage}", e)
          None
      }
    val tmsts = readTmst(ms)
    (dfOpt, TimeRange(ms, tmsts))
  }

  private def dataWhere(): String = {
    if (wheres.length > 0) {
      wheres.mkString(" OR ")
    } else null
  }

}
