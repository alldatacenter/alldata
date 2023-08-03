package com.linkedin.feathr.offline.source.dataloader.jdbc

import org.apache.commons.httpclient.URI
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.spark.sql.{DataFrame, DataFrameReader, SparkSession}

import scala.collection.JavaConverters.asScalaBufferConverter
import java.nio.charset.Charset

/**
 * This is used for Snowflake data source JDBC connector
 *
 */
class SnowflakeDataLoader(ss: SparkSession) {
  val SNOWFLAKE_SOURCE_NAME = "net.snowflake.spark.snowflake"

  def getDFReader(jdbcOptions: Map[String, String]): DataFrameReader = {
    val dfReader = ss.read
      .format(SNOWFLAKE_SOURCE_NAME)
      .options(jdbcOptions)
    dfReader
  }

  def extractSFOptions(ss: SparkSession, url: String): Map[String, String] = {
    var authParams = getSfParams(ss)

    val uri = new URI(url)
    val charset = Charset.forName("UTF-8")
    val params = URLEncodedUtils.parse(uri.getQuery, charset).asScala
    params.foreach(x => {
      authParams = authParams.updated(x.getName, x.getValue)
    })
    authParams
  }

  def getSfParams(ss: SparkSession): Map[String, String] = {
    Map[String, String](
      "sfURL" -> ss.conf.get("sfURL"),
      "sfUser" -> ss.conf.get("sfUser"),
      "sfRole" -> ss.conf.get("sfRole"),
      "sfWarehouse" -> ss.conf.get("sfWarehouse"),
      "sfPassword" -> ss.conf.get("sfPassword"),
    )
  }

   def loadDataFrame(url: String, sfOptions: Map[String, String] = Map[String, String]()): DataFrame = {
    val sparkReader = getDFReader(sfOptions)
    sparkReader
      .load()
  }
}
