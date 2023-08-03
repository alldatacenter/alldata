package com.linkedin.feathr.offline.source.dataloader.jdbc

import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * This Utils contains all
 * Custom Spark Config Keys For JDBC Options
 * Functions to parse JDBC Configs int Spark Conf
 * Functions to get JDBCOptions from Spark Conf
 * Basic Function to load dataframe from JDBC data source (JDBC Data Loader)
 */
object JdbcUtils {
  val DRIVER_CONF = "feathr.jdbc.driver"
  val DBTABLE_CONF = "feathr.jdbc.dbtable"
  val USER_CONF = "feathr.jdbc.user"
  val PASSWORD_CONF = "feathr.jdbc.password"

  val AUTH_FLAG_CONF = "feathr.jdbc.authflag"
  val TOKEN_FLAG = "token"
  val TOKEN_CONF = "feathr.jdbc.token"

  def getJDBCOptionsWithoutAuth(ss: SparkSession): Map[String, String] = {
    Map[String, String]{
      "dbtable" -> ss.conf.get(DBTABLE_CONF)
      "driver" -> ss.conf.getOption(DRIVER_CONF).getOrElse("")
    }
  }

  def getJDBCOptionsWithPassword(ss: SparkSession): Map[String, String] = {
    Map[String, String]{
      "dbtable" -> ss.conf.get(DBTABLE_CONF)
      "user" -> ss.conf.get(USER_CONF)
      "password" -> ss.conf.get(PASSWORD_CONF)
      "driver" -> ss.conf.get(DRIVER_CONF)
    }
  }

  def getJDBCOptionsWithToken(ss: SparkSession): Map[String, String] = {
    Map[String, String]{
      "dbtable" -> ss.conf.get(DBTABLE_CONF)
      "accessToken" -> ss.conf.get(TOKEN_CONF)
      "driver" -> ss.conf.get(DRIVER_CONF)
    }
  }

  def parseJDBCConfigs(sparkConf: SparkConf, dbTable: String, user: String, password: String, driver: String): Unit ={
    sparkConf.set(DBTABLE_CONF, dbTable)
    sparkConf.set(USER_CONF, user)
    sparkConf.set(PASSWORD_CONF, password)
    sparkConf.set(DRIVER_CONF, driver)
  }

  def parseJDBCConfigs(sparkConf: SparkConf, dbTable: String, accessToken: String, driver: String): Unit ={
    sparkConf.set(DBTABLE_CONF, dbTable)
    sparkConf.set(TOKEN_CONF, accessToken)
    sparkConf.set(DRIVER_CONF, driver)
  }

  def loadDataFrame(ss: SparkSession, url: String): DataFrame ={
    val jdbcConnector = JdbcConnectorChooser.getJdbcConnector(ss, url)
    val jdbcOptions = jdbcConnector.extractJdbcOptions(ss, url)
    jdbcConnector.loadDataFrame(url, jdbcOptions)
  }
}
