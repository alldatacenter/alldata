package com.linkedin.feathr.offline.source.dataloader.jdbc

import com.linkedin.feathr.offline.source.dataloader.jdbc.JdbcUtils.getJDBCOptionsWithToken
import org.apache.spark.sql._

abstract class JdbcConnector(ss: SparkSession){
  lazy val _ss: SparkSession = ss
  val format = "jdbc"

  val AUTH_FLAG_CONF = "feathr.jdbc.authflag"
  val TOKEN_FLAG = "token"
  val TOKEN_CONF = "feathr.jdbc.token"

  /**
   * jdbc Options needs to contain necessary information except jdbc url, include:
   * driver option, authentication options, dbTable option, etc.
   * extend a new class when new JDBC data connector is added.
   */
  def getDFReader(jdbcOptions: Map[String, String], url: String): DataFrameReader = {
    _ss.read.format(format).options(jdbcOptions)
  }

  def getDFWriter(df:DataFrame, jdbcOptions: Map[String, String]): DataFrameWriter[Row] = {
    df.write.format(format).options(jdbcOptions)
  }

  def extractJdbcOptions(ss: SparkSession, url: String): Map[String, String] = {
    ss.conf.get(AUTH_FLAG_CONF) match {
      case TOKEN_FLAG => getJDBCOptionsWithToken(ss)
      case _ => JdbcUtils.getJDBCOptionsWithPassword(ss)
    }
  }

  def loadDataFrame(url: String, jdbcOptions: Map[String, String] = Map[String, String]()): DataFrame = {

    val sparkReader = getDFReader(jdbcOptions, url)
    sparkReader
      .option("url", url)
      .load()
  }
}



