package com.linkedin.feathr.offline.config

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.jasonclawson.jackson.dataformat.hocon.HoconFactory
import com.linkedin.feathr.common.FeathrJacksonScalaModule
import com.linkedin.feathr.offline.config.location.{Jdbc, LocationUtils, Snowflake}
import com.linkedin.feathr.offline.source.{DataSource, SourceFormatType}
import org.scalatest.FunSuite

import scala.collection.mutable


class TestDataSourceLoader extends FunSuite {
  /// Base line test to ensure backward compatibility
  test("DataSourceLoader.deserialize BaseLine") {
    val configDoc =
      """
        |{
        |    location: { path: "abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/green_tripdata_2020-04.csv" }
        |    timeWindowParameters: {
        |      timestampColumn: "lpep_dropoff_datetime"
        |      timestampColumnFormat: "yyyy-MM-dd HH:mm:ss"
        |    }
        |}
        |""".stripMargin
    val jackson = new ObjectMapper(new HoconFactory)
      .registerModule(FeathrJacksonScalaModule) // DefaultScalaModule causes a fail on holdem
      .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .registerModule(new SimpleModule().addDeserializer(classOf[DataSource], new DataSourceLoader))
    val ds = jackson.readValue(configDoc, classOf[DataSource])
    assert(ds.path=="abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/green_tripdata_2020-04.csv")
    assert(ds.sourceType == SourceFormatType.FIXED_PATH)
  }

  test("Test Deserialize Snowflake DataSource") {
    val jackson = LocationUtils.getMapper()
    val configDoc =
      """
        |{
        |  location: {
        |    type: "snowflake"
        |    database: "DATABASE"
        |    schema: "SCHEMA"
        |    dbtable: "TABLE"
        |  }
        |  timeWindowParameters: {
        |    timestampColumn: "lpep_dropoff_datetime"
        |    timestampColumnFormat: "yyyy-MM-dd HH:mm:ss"
        |  }
        |}
        |""".stripMargin
    val ds = jackson.readValue(configDoc, classOf[DataSource])
    ds.location match {
      case Snowflake(database, schema, dbtable, query) => {
        assert(database == "DATABASE")
        assert(schema == "SCHEMA")
        assert(dbtable == "TABLE")
      }
      case _ => assert(false)
    }
    assert(ds.timeWindowParams.nonEmpty)
    assert(ds.timePartitionPattern.isEmpty)
    assert(ds.timeWindowParams.get.timestampColumn == "lpep_dropoff_datetime")
    assert(ds.timeWindowParams.get.timestampColumnFormat == "yyyy-MM-dd HH:mm:ss")
  }

  test("Test Deserialize DataSource")     {
    val jackson = LocationUtils.getMapper()
    val configDoc =
      """
        |{
        |  location: {
        |    type: "jdbc"
        |    url: "jdbc:sqlserver://myserver.database.windows.net:1433;database=mydatabase"
        |    user: "bar"
        |    password: "foo"
        |  }
        |  timeWindowParameters: {
        |    timestampColumn: "lpep_dropoff_datetime"
        |    timestampColumnFormat: "yyyy-MM-dd HH:mm:ss"
        |  }
        |}
        |""".stripMargin
    val ds = jackson.readValue(configDoc, classOf[DataSource])
    ds.location match {
      case Jdbc(url, dbtable, user, password, token) => {
        assert(url == "jdbc:sqlserver://myserver.database.windows.net:1433;database=mydatabase")
        assert(user=="bar")
        assert(password=="foo")
      }
      case _ => assert(false)
    }
    assert(ds.timeWindowParams.nonEmpty)
    assert(ds.timePartitionPattern.isEmpty)
    assert(ds.timeWindowParams.get.timestampColumn=="lpep_dropoff_datetime")
    assert(ds.timeWindowParams.get.timestampColumnFormat=="yyyy-MM-dd HH:mm:ss")
  }

  test("test SWA with dense vector feature") {
    val sps = """{"nycTaxiBatchJdbcSource_USER": "user@host", "nycTaxiBatchJdbcSource_PASSWORD": "magic-word"}"""
    val props = (new ObjectMapper()).registerModule(DefaultScalaModule).readValue(sps, classOf[mutable.HashMap[String, String]])
    assert(props.get("nycTaxiBatchJdbcSource_PASSWORD")==Some("magic-word"))
  }
}
