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

import org.apache.spark.sql.{DataFrame, SparkSession}

import org.apache.griffin.measure.configuration.dqdefinition.DataConnectorParam
import org.apache.griffin.measure.context.TimeRange
import org.apache.griffin.measure.datasource.TimestampStorage
import org.apache.griffin.measure.utils.ParamUtil._

@deprecated(
  s"This class is deprecated. Use '${classOf[JDBCBasedDataConnector].getCanonicalName}'.",
  "0.6.0")
case class MySqlDataConnector(
    @transient sparkSession: SparkSession,
    dcParam: DataConnectorParam,
    timestampStorage: TimestampStorage)
    extends BatchDataConnector {

  val Database = "database"
  val TableName = "table.name"
  val Where = "where"
  val Url = "url"
  val User = "user"
  val Password = "password"
  val Driver = "driver"

  val database: String = dcParam.getConfig.getString(Database, "default")
  val tableName: String = dcParam.getConfig.getString(TableName, "")
  val fullTableName: String = s"$database.$tableName"
  val whereString: String = dcParam.getConfig.getString(Where, "")
  val url: String = dcParam.getConfig.getString(Url, "")
  val user: String = dcParam.getConfig.getString(User, "")
  val password: String = dcParam.getConfig.getString(Password, "")
  val driver: String = dcParam.getConfig.getString(Driver, "com.mysql.jdbc.Driver")

  override def data(ms: Long): (Option[DataFrame], TimeRange) = {

    val dfOpt =
      try {
        val dtSql = dataSql()
        val prop = new java.util.Properties
        prop.setProperty("user", user)
        prop.setProperty("password", password)
        prop.setProperty("driver", driver)
        val df: DataFrame = sparkSession.read.jdbc(url, s"($dtSql) as t", prop)
        val dfOpt = Some(df)
        val preDfOpt = preProcess(dfOpt, ms)
        preDfOpt
      } catch {
        case e: Throwable =>
          error(s"load mysql table $fullTableName fails: ${e.getMessage}", e)
          None
      }
    val tmsts = readTmst(ms)
    (dfOpt, TimeRange(ms, tmsts))
  }

  private def dataSql(): String = {

    val wheres = whereString.split(",").map(_.trim).filter(_.nonEmpty)
    val tableClause = s"SELECT * FROM $fullTableName"
    if (wheres.length > 0) {
      val clauses = wheres.map { w =>
        s"$tableClause WHERE $w"
      }
      clauses.mkString(" UNION ALL ")
    } else tableClause
  }
}
