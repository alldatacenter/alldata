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

import java.io.{BufferedReader, ByteArrayInputStream, InputStreamReader}
import java.net.URI
import java.util.{Iterator => JavaIterator, Map => JavaMap}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet, HttpPost, HttpRequestBase}
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.{BasicResponseHandler, HttpClientBuilder}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, DataFrameReader, Row, SparkSession}
import org.apache.spark.sql.types.{StructField, StructType}

import org.apache.griffin.measure.configuration.dqdefinition.DataConnectorParam
import org.apache.griffin.measure.context.TimeRange
import org.apache.griffin.measure.datasource.TimestampStorage
import org.apache.griffin.measure.utils.ParamUtil._

@deprecated(
  s"This class is deprecated. Use '${classOf[ElasticSearchDataConnector].getCanonicalName}'.",
  "0.6.0")
case class ElasticSearchGriffinDataConnector(
    @transient sparkSession: SparkSession,
    dcParam: DataConnectorParam,
    timestampStorage: TimestampStorage)
    extends BatchDataConnector {

  lazy val getBaseUrl = s"http://$host:$port"
  val config: scala.Predef.Map[scala.Predef.String, scala.Any] = dcParam.getConfig
  val Index = "index"
  val Type = "type"
  val Host = "host"
  val Port = "port"
  val EsVersion = "version"
  val Fields = "fields"
  val Size = "size"
  val MetricName = "metric.name"
  val Sql = "sql"
  val SqlMode = "sql.mode"
  val index: String = config.getString(Index, "default")
  val dataType: String = config.getString(Type, "accuracy")
  val metricName: String = config.getString(MetricName, "*")
  val host: String = config.getString(Host, "")
  val version: String = config.getString(EsVersion, "")
  val port: String = config.getString(Port, "")
  val fields: Seq[String] = config.getStringArr(Fields, Seq[String]())
  val sql: String = config.getString(Sql, "")
  val sqlMode: Boolean = config.getBoolean(SqlMode, defValue = false)
  val size: Int = config.getInt(Size, 100)

  override def data(ms: Long): (Option[DataFrame], TimeRange) = {
    if (sqlMode) dataBySql(ms) else dataBySearch(ms)
  }

  def dataBySql(ms: Long): (Option[DataFrame], TimeRange) = {
    val path: String = "/_sql?format=csv"
    info(s"ElasticSearchGriffinDataConnector data : sql: $sql")
    val dfOpt =
      try {
        val answer = httpPost(path, sql)
        if (answer._1) {
          import sparkSession.implicits._
          val rdd: RDD[String] = sparkSession.sparkContext.parallelize(answer._2.lines.toList)
          val reader: DataFrameReader = sparkSession.read
          reader.option("header", value = true).option("inferSchema", value = true)
          val df: DataFrame = reader.csv(rdd.toDS())
          val dfOpt = Some(df)
          val preDfOpt = preProcess(dfOpt, ms)
          preDfOpt
        } else None
      } catch {
        case e: Throwable =>
          error(s"load ES by sql $host:$port $sql  fails: ${e.getMessage}", e)
          None
      }
    val tmsts = readTmst(ms)
    (dfOpt, TimeRange(ms, tmsts))
  }

  def dataBySearch(ms: Long): (Option[DataFrame], TimeRange) = {
    val path: String = s"/$index/$dataType/_search?sort=tmst:desc&q=name:$metricName&size=$size"
    info(s"ElasticSearchGriffinDataConnector data : host: $host port: $port path:$path")

    val dfOpt =
      try {
        val answer = httpGet(path)
        val data = ArrayBuffer[Map[String, Number]]()

        if (answer._1) {
          val arrayAnswers: JavaIterator[JsonNode] =
            parseString(answer._2).get("hits").get("hits").elements()

          while (arrayAnswers.hasNext) {
            val answer = arrayAnswers.next()
            val values = answer.get("_source").get("value")
            val fields: JavaIterator[JavaMap.Entry[String, JsonNode]] = values.fields()
            val fieldsMap = mutable.Map[String, Number]()
            while (fields.hasNext) {
              val fld: JavaMap.Entry[String, JsonNode] = fields.next()
              fieldsMap.put(fld.getKey, fld.getValue.numberValue())
            }
            data += fieldsMap.toMap
          }
        }
        val rdd1: RDD[Map[String, Number]] = sparkSession.sparkContext.parallelize(data)
        val columns: Array[String] = fields.toArray
        val defaultNumber: Number = 0.0
        val rdd: RDD[Row] = rdd1
          .map { x: Map[String, Number] =>
            Row(columns.map(c => x.getOrElse(c, defaultNumber).doubleValue()): _*)
          }
        val schema = dfSchema(columns.toList)
        val df: DataFrame = sparkSession.createDataFrame(rdd, schema).limit(size)
        val dfOpt = Some(df)
        val preDfOpt = preProcess(dfOpt, ms)
        preDfOpt
      } catch {
        case e: Throwable =>
          error(s"load ES table $host:$port $index/$dataType  fails: ${e.getMessage}", e)
          None
      }
    val tmsts = readTmst(ms)
    (dfOpt, TimeRange(ms, tmsts))
  }

  def httpGet(path: String): (Boolean, String) = {
    val url: String = s"$getBaseUrl$path"
    info(s"url:$url")
    val uri: URI = new URI(url)
    val request = new HttpGet(uri)
    doRequest(request)
  }

  def httpPost(path: String, body: String): (Boolean, String) = {
    val url: String = s"$getBaseUrl$path"
    info(s"url:$url")
    val uri: URI = new URI(url)
    val request = new HttpPost(uri)
    request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON))
    doRequest(request)
  }

  def doRequest(request: HttpRequestBase): (Boolean, String) = {
    request.addHeader("Content-Type", "application/json")
    request.addHeader("Charset", "UTF-8")
    val client = HttpClientBuilder.create().build()
    val response: CloseableHttpResponse = client.execute(request)
    val handler = new BasicResponseHandler()
    (true, handler.handleResponse(response).trim)
  }

  def parseString(data: String): JsonNode = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    val reader = new BufferedReader(
      new InputStreamReader(new ByteArrayInputStream(data.getBytes)))
    mapper.readTree(reader)
  }

  def dfSchema(columnNames: List[String]): StructType = {
    val a: Seq[StructField] = columnNames
      .map(x =>
        StructField(name = x, dataType = org.apache.spark.sql.types.DoubleType, nullable = false))
    StructType(a)
  }

}
