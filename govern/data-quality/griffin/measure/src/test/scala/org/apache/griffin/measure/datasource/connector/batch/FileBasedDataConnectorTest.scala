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

import org.apache.spark.sql.types.{IntegerType, LongType, StringType, StructType}
import org.scalatest.matchers.should._

import org.apache.griffin.measure.SparkSuiteBase
import org.apache.griffin.measure.configuration.dqdefinition.DataConnectorParam
import org.apache.griffin.measure.datasource.TimestampStorage
import org.apache.griffin.measure.step.builder.ConstantColumns

class FileBasedDataConnectorTest extends SparkSuiteBase with Matchers {

  private final val dcParam =
    DataConnectorParam("file", "test_df", Map.empty[String, String], Nil)
  private final val timestampStorage = TimestampStorage()

  "file based data connector" should "be able to read from local filesystem" in {
    val configs = Map(
      "format" -> "csv",
      "paths" -> Seq(s"file://${getClass.getResource("/hive/person_table.csv").getPath}"),
      "options" -> Map("header" -> "false"))

    val dc = FileBasedDataConnector(spark, dcParam.copy(config = configs), timestampStorage)
    val result = dc.data(1000L)

    assert(result._1.isDefined)
    assert(result._1.get.collect().length == 2)
  }

  // Regarding User Defined Schema

  it should "respect the provided schema, if any" in {
    val configs = Map(
      "format" -> "csv",
      "paths" -> Seq(s"file://${getClass.getResource("/hive/person_table.csv").getPath}"))

    // no schema
    assertThrows[IllegalArgumentException](
      FileBasedDataConnector(spark, dcParam.copy(config = configs), timestampStorage))

    // invalid schema
    assertThrows[IllegalStateException](
      FileBasedDataConnector(
        spark,
        dcParam.copy(config = configs + (("schema", ""))),
        timestampStorage))

    // valid schema
    val result1 = FileBasedDataConnector(
      spark,
      dcParam.copy(config = configs + (
        (
          "schema",
          Seq(
            Map("name" -> "name", "type" -> "string"),
            Map("name" -> "age", "type" -> "int", "nullable" -> "true"))))),
      timestampStorage)
      .data(1L)

    val expSchema = new StructType()
      .add("name", StringType)
      .add("age", IntegerType, nullable = true)
      .add(ConstantColumns.tmst, LongType, nullable = false)

    assert(result1._1.isDefined)
    assert(result1._1.get.collect().length == 2)
    assert(result1._1.get.schema == expSchema)

    // valid headers
    val result2 = FileBasedDataConnector(
      spark,
      dcParam.copy(config = configs + (("options", Map("header" -> "true")))),
      timestampStorage)
      .data(1L)

    assert(result2._1.isDefined)
    assert(result2._1.get.collect().length == 1)
    result2._1.get.columns should contain theSameElementsAs Seq(
      "Joey",
      "14",
      ConstantColumns.tmst)
  }

  // skip on erroneous paths

  it should "respect options if an erroneous path is encountered" in {
    val configs = Map(
      "format" -> "csv",
      "paths" -> Seq(
        s"file://${getClass.getResource("/hive/person_table.csv").getPath}",
        s"${java.util.UUID.randomUUID().toString}/"),
      "skipErrorPaths" -> true,
      "options" -> Map("header" -> "true"))

    // valid paths
    val result1 =
      FileBasedDataConnector(spark, dcParam.copy(config = configs), timestampStorage).data(1L)

    assert(result1._1.isDefined)
    assert(result1._1.get.collect().length == 1)

    // non existent path
    assertThrows[IllegalArgumentException](
      FileBasedDataConnector(
        spark,
        dcParam.copy(config = configs - "skipErrorPaths"),
        timestampStorage).data(1L))

    // no path
    assertThrows[AssertionError](
      FileBasedDataConnector(spark, dcParam.copy(config = configs - "paths"), timestampStorage)
        .data(1L))
  }

  // Regarding various formats
  it should "be able to read all supported file types" in {

    val formats = Seq("parquet", "orc", "csv", "tsv")
    formats.map(f => {
      val configs = Map(
        "format" -> f,
        "paths" -> Seq(s"file://${getClass.getResource(s"/files/person_table.$f").getPath}"),
        "options" -> Map("header" -> "true", "inferSchema" -> "true"))

      val result =
        FileBasedDataConnector(spark, dcParam.copy(config = configs), timestampStorage).data(1L)

      assert(result._1.isDefined)

      val df = result._1.get
      val expSchema = new StructType()
        .add("name", StringType)
        .add("age", IntegerType, nullable = true)
        .add(ConstantColumns.tmst, LongType, nullable = false)

      assert(df.collect().length == 2)
      assert(df.schema == expSchema)
    })
  }

  it should "apply schema to all formats if provided" in {
    val formats = Seq("parquet", "orc", "csv", "tsv")
    formats.map(f => {
      val configs = Map(
        "format" -> f,
        "paths" -> Seq(
          s"file://${ClassLoader.getSystemResource(s"files/person_table.$f").getPath}"),
        "options" -> Map("header" -> "true"),
        "schema" -> Seq(Map("name" -> "name", "type" -> "string")))

      val result =
        FileBasedDataConnector(spark, dcParam.copy(config = configs), timestampStorage).data(1L)

      assert(result._1.isDefined)

      val df = result._1.get
      val expSchema = new StructType()
        .add("name", StringType)
        .add(ConstantColumns.tmst, LongType, nullable = false)

      assert(df.collect().length == 2)
      assert(df.schema == expSchema)
    })
  }

}
