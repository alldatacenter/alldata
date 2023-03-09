package com.linkedin.feathr.offline.config.location

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.caseclass.mapper.CaseClassObjectMapper
import com.jasonclawson.jackson.dataformat.hocon.HoconFactory
import com.linkedin.feathr.common.FeathrJacksonScalaModule
import com.linkedin.feathr.offline.config.DataSourceLoader
import com.linkedin.feathr.offline.config.location.LocationUtils.envSubstitute
import com.linkedin.feathr.offline.config.location.{DataLocation, Jdbc, SimplePath}
import com.linkedin.feathr.offline.generation.SparkIOUtils
import com.linkedin.feathr.offline.source.DataSource
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession
import org.apache.hadoop.mapred.JobConf
import org.scalatest.FunSuite

class TestDesLocation extends FunSuite {
  val jackson = (new ObjectMapper(new HoconFactory) with CaseClassObjectMapper)
    .registerModule(FeathrJacksonScalaModule) // DefaultScalaModule causes a fail on holdem
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .registerModule(new SimpleModule().addDeserializer(classOf[DataSource], new DataSourceLoader))

  test("envSubstitute") {
    scala.util.Properties.setProp("PROP1", "foo")
    assert(envSubstitute("${PROP1}") == "foo")
    assert(envSubstitute("${PROP1}abc") == "fooabc")
    assert(envSubstitute("xyz${PROP1}abc") == "xyzfooabc")
  }

  test("Deserialize Location") {
    {
      val configDoc = """{ path: "abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/green_tripdata_2020-04.csv" }"""
      val ds = jackson.readValue(configDoc, classOf[DataLocation])
      ds match {
        case SimplePath(path) => {
          assert(path == "abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/green_tripdata_2020-04.csv")
        }
        case _ => assert(false)
      }
    }

    {
      val configDoc =
        """
          |{
          | type: "jdbc"
          | url: "jdbc:sqlserver://myserver.database.windows.net:1433;database=mydatabase"
          | user: "bar"
          | password: "foo"
          |}""".stripMargin
      val ds = jackson.readValue(configDoc, classOf[DataLocation])
      ds match {
        case Jdbc(url, dbtable, user, password, token) => {
          assert(url == "jdbc:sqlserver://myserver.database.windows.net:1433;database=mydatabase")
          assert(user == "bar")
          assert(password == "foo")
        }
        case _ => assert(false)
      }
    }
  }

  test("Deserialize PathList") {
    val configDoc =
      """
        |{
        | type: "pathlist"
        | paths: ["abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/green_tripdata_2020-04.csv"]
        |}""".stripMargin
    val ds = jackson.readValue(configDoc, classOf[DataLocation])
    ds match {
      case PathList(pathList) => {
        assert(pathList == List("abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/green_tripdata_2020-04.csv"))
      }
      case _ => assert(false)
    }
  }

  test("Deserialize Snowflake") {
    val configDoc =
      """
        |{
        | type: "snowflake"
        | dbtable: "TABLE"
        | database: "DATABASE"
        | schema: "SCHEMA"
        |}""".stripMargin
    val ds = jackson.readValue(configDoc, classOf[Snowflake])
    ds match {
      case Snowflake(database, schema, dbtable, query) => {
        assert(database == "DATABASE")
        assert(schema == "SCHEMA")
        assert(dbtable == "TABLE")
      }
      case _ => assert(false)
    }
  }

  test("Test load Sqlite") {
    val path = s"${System.getProperty("user.dir")}/src/test/resources/mockdata/sqlite/test.db"
    val configDoc =
      s"""
        |{
        | type: "jdbc"
        | url: "jdbc:sqlite:${path}"
        | dbtable: "table1"
        | anonymous: true
        |}""".stripMargin
    val ds = jackson.readValue(configDoc, classOf[DataLocation])

    val _ = SparkSession.builder().config("spark.master", "local").appName("Sqlite test").getOrCreate()

    val df = SparkIOUtils.createDataFrame(ds, Map(), new JobConf(), List())
    val rows = df.head(3)
    assert(rows(0).getLong(0) == 1)
    assert(rows(1).getLong(0) == 2)
    assert(rows(2).getLong(0) == 3)
    assert(rows(0).getString(1) == "r1c2")
    assert(rows(1).getString(1) == "r2c2")
    assert(rows(2).getString(1) == "r3c2")
  }

  test("Test load Sqlite with SQL query") {
    val path = s"${System.getProperty("user.dir")}/src/test/resources/mockdata/sqlite/test.db"
    val configDoc =
      s"""
         |{
         | type: "jdbc"
         | url: "jdbc:sqlite:${path}"
         | query: "select c1, c2 from table1"
         | anonymous: true
         |}""".stripMargin
    val ds = jackson.readValue(configDoc, classOf[DataLocation])

    val _ = SparkSession.builder().config("spark.master", "local").appName("Sqlite test").getOrCreate()

    val df = SparkIOUtils.createDataFrame(ds, Map(), new JobConf(), List())
    val rows = df.head(3)
    assert(rows(0).getLong(0) == 1)
    assert(rows(1).getLong(0) == 2)
    assert(rows(2).getLong(0) == 3)
    assert(rows(0).getString(1) == "r1c2")
    assert(rows(1).getString(1) == "r2c2")
    assert(rows(2).getString(1) == "r3c2")
  }

  test("Test GenericLocation load Sqlite with SQL query") {
    val path = s"${System.getProperty("user.dir")}/src/test/resources/mockdata/sqlite/test.db"
    val configDoc =
      s"""
         |{
         | type: "generic"
         | format: "jdbc"
         | url: "jdbc:sqlite:${path}"
         | query: "select c1, c2 from table1"
         |}""".stripMargin
    val ds = jackson.readValue(configDoc, classOf[DataLocation])

    val _ = SparkSession.builder().config("spark.master", "local").appName("Sqlite test").getOrCreate()

    val df = SparkIOUtils.createDataFrame(ds, Map(), new JobConf(), List())
    val rows = df.head(3)
    assert(rows(0).getLong(0) == 1)
    assert(rows(1).getLong(0) == 2)
    assert(rows(2).getLong(0) == 3)
    assert(rows(0).getString(1) == "r1c2")
    assert(rows(1).getString(1) == "r2c2")
    assert(rows(2).getString(1) == "r3c2")
  }
}

