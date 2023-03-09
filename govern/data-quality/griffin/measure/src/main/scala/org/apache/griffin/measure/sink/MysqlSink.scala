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

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import java.sql.{Connection, DriverManager}
import java.util
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame
import scala.concurrent.Future

import org.apache.griffin.measure.Loggable
import org.apache.griffin.measure.utils.{JsonUtil, ParamUtil, TimeUtil}
import org.apache.griffin.measure.utils.ParamUtil.ParamMap

/**
 * sink metric and record to mysql
 * example mysql sink result:
 * {"tmst": "1652606473604",
 *  "metrics": "Map(total -> 3, miss -> 0, matched -> 3, matchedFraction -> 1.0)",
 * "job_name": "test0516_03",
 * "applicationId": "application_1651974446346_0933"}
 */
case class MysqlSink(config: Map[String, Any], jobName: String, timeStamp: Long, block: Boolean)
    extends Sink
    with Loggable {

  val OverTime = "over.time"
  val Retry = "retry"
  val overTime: Long = TimeUtil.milliseconds(config.getString(OverTime, "")).getOrElse(-1L)
  val retry: Int = config.getInt(Retry, 10)
  var connection: Connection = MysqlConnection.getMysqlConn(config)

  override def sinkRecords(records: RDD[String], name: String): Unit = {}

  override def sinkRecords(records: Iterable[String], name: String): Unit = {}

  override def sinkMetrics(metrics: Map[String, Any]): Unit = {
    griffinLogger.info("start sink metric to mysql, sinkMysqlMetrics..\n" + metrics)
    mysqlInsert(metrics, connection)
  }

  /**
   * dataMap = {
   * "tmst": "1652606473604",
   * "metrics": { "total" -> 3, "miss" -> 0, "matched" -> 3, "matchedFraction" -> 1.0}",
   * "job_name": "test0516_03",
   * "applicationId": "application_1651974446346_0933"
   * "metadata": { "applicationId": "application_1651974446346_0933", "owner": "test"}
   * }
   * @param dataMap
   * @param connection
   */
  private def mysqlInsert(dataMap: Map[String, Any], connection: Connection): Unit = {
    val timeStamp: Long = dataMap("tmst").toString.toLong
    val timeStampString: String = dataMap("tmst").toString
    val jobName: String = dataMap("job_name").toString
    val applicationId: String = dataMap("applicationId").toString
    val mapper = new ObjectMapper()
    val metricsResult = JsonUtil.toJson(dataMap("metrics"))
    griffinLogger.info(metricsResult)
    val metadata = dataMap.getOrElse("metadata", "").toString
    var metadataResult = ""
    if (metadata.nonEmpty) {
      val metadataMap: util.Map[String, String] = MysqlConnection.mapStringToMap(metadata)
      metadataMap.put("applicationId", applicationId)
      metadataResult = mapper.writeValueAsString(metadataMap)
      info(metadataResult)
    } else {
      val metadataMap: util.Map[String, String] = new util.HashMap[String, String]()
      metadataMap.put("applicationId", applicationId)
      metadataResult = mapper.writeValueAsString(metadataMap)
    }
    griffinLogger.info("Begin Mysql Insert.. dataMap:\n" + dataMap)
    griffinLogger.info("timeStamp:\n" + timeStamp)
    griffinLogger.info("timeStampString:\n" + timeStampString)
    griffinLogger.info("metricResult:\n" + metricsResult)
    griffinLogger.info("jobName:\n" + jobName)
    griffinLogger.info("applicationId:\n" + applicationId)
    griffinLogger.info("metadata save to Mysql:\n" + metadataResult)

    try {
      def func(): (Long, Future[Boolean]) = {
        import scala.concurrent.ExecutionContext.Implicits.global
        try {
          val prep = connection.prepareStatement(
            "INSERT INTO mysql_sink (tmst, value, job_name, application_id, metadata) VALUES (?,?,?,?,?) ")
          prep.setObject(1, dataMap("tmst").toString)
          prep.setObject(2, metricsResult)
          prep.setObject(3, jobName)
          prep.setObject(4, applicationId)
          prep.setObject(5, metadataResult)
          prep.executeUpdate
        } catch {
          case e: Throwable =>
            griffinLogger.info("func Mysql Sink Error.. json:\n" + e.getMessage)
            error(e.getMessage, e)
        } finally {
          if (connection != null) {
            connection.close
          }
        }
        griffinLogger.info(
          "func Mysql Sink Success.. metrics:\n" + metricsResult + "\nmetadata: \n" + metadataResult)
        (timeStamp, Future(true))
      }
      if (block) SinkTaskRunner.addBlockTask(func _, retry, overTime)
      else SinkTaskRunner.addNonBlockTask(func _, retry)
    } catch {
      case e: Throwable =>
        griffinLogger.info("Griffin Mysql Sink mysqlInsert Failed..:\n" + e.getMessage)
        error(e.getMessage, e)
    }
    griffinLogger.info(
      "Griffin Mysql Sink Success.. metrics:\n" + metricsResult + "\nmetadata: \n" + metadataResult)

  }

  override def sinkBatchRecords(dataset: DataFrame, key: Option[String] = None): Unit = {}

  /**
   * Ensures that the pre-requisites (if any) of the Sink are met before opening it.
   */
  override def validate(): Boolean = {
    true
  }

}

object MysqlConnection extends Loggable {

  var mysqlConf: Map[String, String] = Map.empty
  private var initialed = false
  var connection: Connection = _

  def getMysqlConn(config: Map[String, Any]): Connection = {
    if (!initialed) {
      mysqlConf += ("driver" -> config("driver").toString)
      mysqlConf += ("url" -> config("url").toString)
      mysqlConf += ("username" -> config("username").toString)
      mysqlConf += ("password" -> config("password").toString)
      //      griffinLogger.info("getMysqlConnection...\n" + mysqlConf)
      Class.forName(mysqlConf("driver"))
      initialed = true
    }
    DriverManager.getConnection(mysqlConf("url"), mysqlConf("username"), mysqlConf("password"))
  }

  def parse(json: String): Unit = {
    val factory = new JsonFactory
    val mapper = new ObjectMapper(factory)
    val rootNode: JsonNode = mapper.readTree(json)
    val fieldsIterator = rootNode.fields
    while (fieldsIterator.hasNext) {
      val field = fieldsIterator.next
      System.out.println("Key: " + field.getKey + "\tValue:" + field.getValue)
    }
  }

  def mapStringToMap(str: String): util.Map[String, String] = {
    var result = str
    if (result.contains("Map(")) {
      result = result.replace("Map(", "")
      result = result.replace(")", "")
    }
    val mapString = result.split(",")
    val map = new util.HashMap[String, String]
    for (string <- mapString) {
      val key = string.split("->")(0).trim
      val value = string.split("->")(1).trim
      map.put(key, value)
    }
    map
  }

}

object RunAppDemo {
  def main(args: Array[String]) {

    val mysqlConf = Map(
      "driver" -> "com.mysql.jdbc.Driver",
      "url" -> "jdbc:mysql://localhost:3306/mysql_sink",
      "username" -> "root",
      "password" -> "123456")

    val mysqlSink: MysqlSink = MysqlSink(mysqlConf, "test1and2_second", 1652340184501L, true)
    mysqlSink.sinkMetrics(null)

    val realResult = MysqlConnection.mapStringToMap(
      "Map(total -> 3, miss -> 0, matched -> 3, matchedFraction -> 1.0)")
    println(realResult.get("total"))
    println(realResult.get("miss"))
    println(realResult.get("matched"))
    println(realResult.get("matchedFraction"))
  }
}
