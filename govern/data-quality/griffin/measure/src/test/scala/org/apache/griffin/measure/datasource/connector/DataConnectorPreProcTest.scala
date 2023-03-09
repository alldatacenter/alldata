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

package org.apache.griffin.measure.datasource.connector

import java.io.File
import java.time.ZonedDateTime

import scala.io.Source
import scala.util.Try

import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.types.StructType
import org.scalatest.matchers.should.Matchers

import org.apache.griffin.measure.configuration.dqdefinition.DataConnectorParam
import org.apache.griffin.measure.context.TimeRange
import org.apache.griffin.measure.datasource.TimestampStorage
import org.apache.griffin.measure.datasource.connector.batch.{
  BatchDataConnector,
  FileBasedDataConnector
}
import org.apache.griffin.measure.SparkSuiteBase

case class SampleBatchDataConnector(
    @transient sparkSession: SparkSession,
    dcParam: DataConnectorParam,
    timestampStorage: TimestampStorage)
    extends BatchDataConnector {

  val rawData: Seq[Row] = Seq(
    "2015-05-26T00:26:00Z,PENAL CODE/MISC (PENALMI),3900 Block BLOCK EL CAMINO REAL,PALO ALTO,94306",
    "2015-05-26T00:26:00Z,DRUNK IN PUBLIC ADULT/MISC (647FA),3900 Block BLOCK EL CAMINO REAL,PALO ALTO,94306",
    "2015-05-26T00:26:00Z,PENAL CODE/MISC (PENALMI),3900 Block BLOCK EL CAMINO REAL,PALO ALTO,94306",
    "2015-05-26T00:26:00Z,PENAL CODE/MISC (PENALMI),3900 Block BLOCK EL CAMINO REAL,PALO ALTO,94306",
    "2015-05-26T02:30:00Z,N&D/POSSESSION (11350),WILKIE WAY & JAMES RD,PALO ALTO,94306")
    .map(str => Row(str))

  override def data(ms: Long): (Option[DataFrame], TimeRange) = {
    val schema = new StructType().add("value", "string", nullable = true)

    val df = sparkSession.createDataset(rawData)(RowEncoder(schema))
    (preProcess(Option(df), ms), TimeRange(ms))
  }
}

class DataConnectorPreProcTest extends SparkSuiteBase with Matchers {

  private def castSeq(s: Seq[Any]): Seq[Any] = {

    val arr = s.toArray
    arr(0) =
      Try(java.sql.Timestamp.from(ZonedDateTime.parse(s(0).toString).toInstant)).getOrElse(null)
    arr(4) = Try(s(4).toString.toInt).getOrElse(null)

    arr.toSeq
  }

  "DataConnector" should "return input data if no pre.proc rules are applied" in {
    val param = DataConnectorParam(
      "CUSTOM",
      null,
      Map("class" -> classOf[SampleBatchDataConnector].getCanonicalName),
      Nil)

    val timestampStorage = TimestampStorage()
    val ts = 0L

    val dc = DataConnectorFactory.getDataConnector(spark, null, param, timestampStorage, None)
    assert(dc.isSuccess)
    assert(dc.toOption.isDefined)
    assert(dc.get.isInstanceOf[SampleBatchDataConnector])
    assert(dc.get.data(ts)._1.isDefined)

    val expectedRows =
      dc.get
        .asInstanceOf[SampleBatchDataConnector]
        .rawData
        .map(x => Row(x.toSeq.:+(ts): _*))
    val actualRows = dc.get.data(ts)._1.get.collect()

    actualRows should contain theSameElementsAs expectedRows
    assert(actualRows.length == expectedRows.length)
  }

  it should "apply pre.proc rule if provided" in {
    val param = DataConnectorParam(
      "CUSTOM",
      null,
      Map("class" -> classOf[SampleBatchDataConnector].getCanonicalName),
      List("select split(value, ',') as part from this"))

    val timestampStorage = TimestampStorage()
    val ts = 0L

    val dc = DataConnectorFactory.getDataConnector(spark, null, param, timestampStorage, None)
    assert(dc.isSuccess)
    assert(dc.toOption.isDefined)
    assert(dc.get.isInstanceOf[SampleBatchDataConnector])
    assert(dc.get.data(ts)._1.isDefined)

    val expectedRows = dc.get
      .asInstanceOf[SampleBatchDataConnector]
      .rawData
      .map(x => Row(x.getString(0).split(",").toSeq, ts))
    val actualRows = dc.get.data(ts)._1.get.collect()

    actualRows should contain theSameElementsAs expectedRows
    assert(actualRows.length == expectedRows.length)
  }

  it should "chain pre.proc rules in sequence if more than one rule is provided" in {
    val param = DataConnectorParam(
      "CUSTOM",
      null,
      Map("class" -> classOf[SampleBatchDataConnector].getCanonicalName),
      List(
        "select split(value, ',') as part from this",
        "select part[0] as date_time, part[1] as incident, part[2] as address, " +
          "part[3] as city, part[4] as zipcode from this",
        "select cast(date_time as timestamp) as date_time, incident, address, city, " +
          "cast(zipcode as int) as zipcode from this"))

    val timestampStorage = TimestampStorage()
    val ts = 0L

    val dc = DataConnectorFactory.getDataConnector(spark, null, param, timestampStorage, None)
    assert(dc.isSuccess)
    assert(dc.toOption.isDefined)
    assert(dc.get.isInstanceOf[SampleBatchDataConnector])
    assert(dc.get.data(ts)._1.isDefined)

    val expectedRows = dc.get
      .asInstanceOf[SampleBatchDataConnector]
      .rawData
      .map(x => Row(x.getString(0).split(",").toSeq))
      .map(r => Row(castSeq(r.getSeq(0)).:+(ts): _*))
    val actualRows = dc.get.data(ts)._1.get.collect()

    actualRows should contain theSameElementsAs expectedRows
    assert(actualRows.length == expectedRows.length)
  }

  it should "pre process data from other connectors as well" in {
    val dataFilePath = "src/test/resources/crime_report_test.csv"

    val param = DataConnectorParam(
      "file",
      null,
      Map(
        "format" -> "csv",
        "paths" -> Seq(dataFilePath),
        "schema" -> Seq(
          Map("name" -> "date_time", "type" -> "string"),
          Map("name" -> "incident", "type" -> "string"),
          Map("name" -> "address", "type" -> "string"),
          Map("name" -> "city", "type" -> "string"),
          Map("name" -> "zipcode", "type" -> "string"))),
      List(
        "select cast(date_time as timestamp) as date_time, incident, address, city, " +
          "cast(zipcode as int) as zipcode from this"))

    val timestampStorage = TimestampStorage()
    val ts = 0L

    val dc = DataConnectorFactory.getDataConnector(spark, null, param, timestampStorage, None)

    assert(dc.isSuccess)
    assert(dc.toOption.isDefined)
    assert(dc.get.isInstanceOf[FileBasedDataConnector])
    assert(dc.get.data(ts)._1.isDefined)

    val source = Source.fromFile(new File(dataFilePath))

    val expectedRows = source
      .getLines()
      .toSeq
      .map(x => Row(x.split(",").toSeq))
      .map(r => Row(castSeq(r.getSeq(0)).:+(ts): _*))
    val actualRows = dc.get.data(ts)._1.get.collect()

    actualRows should contain theSameElementsAs expectedRows
    assert(actualRows.length == expectedRows.length)
  }
}
