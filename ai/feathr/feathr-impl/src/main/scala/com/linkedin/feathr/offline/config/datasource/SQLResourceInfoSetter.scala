package com.linkedin.feathr.offline.config.datasource

import com.linkedin.feathr.offline.source.dataloader.jdbc.JdbcUtils
import org.apache.spark.SparkConf

private[feathr] class SQLResourceInfoSetter extends ResourceInfoSetter() {
  val JDBC_TABLE = "JDBC_TABLE"
  val JDBC_USER = "JDBC_USER"
  val JDBC_PASSWORD = "JDBC_PASSWORD"
  val JDBC_DRIVER = "JDBC_DRIVER"
  val JDBC_TOKEN = "JDBC_TOKEN"
  val JDBC_AUTH_FLAG = "JDBC_AUTH_FLAG"

  override val params = List(JDBC_TABLE, JDBC_USER, JDBC_PASSWORD, JDBC_DRIVER, JDBC_TOKEN, JDBC_AUTH_FLAG)

  def setupSparkConf(sparkConf: SparkConf, context: Option[DataSourceConfig], resource: Option[Resource]): Unit = {
    val table = getAuthStr(JDBC_TABLE, context, resource)
    val driver = getAuthStr(JDBC_DRIVER, context, resource)
    val authFlag = getAuthStr(JDBC_AUTH_FLAG, context, resource)
    val accessToken = getAuthStr(JDBC_TOKEN, context, resource)
    val user = getAuthStr(JDBC_USER, context, resource)
    val password = getAuthStr(JDBC_PASSWORD, context, resource)

    authFlag match {
      case JdbcUtils.TOKEN_FLAG => JdbcUtils.parseJDBCConfigs(sparkConf, table, accessToken, driver)
      case _ => JdbcUtils.parseJDBCConfigs(sparkConf, table, user, password, driver)
    }
  }


  def getAuthFromConfig(str: String, resource: Resource): String = {
    str match {
      case JDBC_TABLE => resource.azureResource.jdbcTable
      case JDBC_USER => resource.azureResource.jdbcUser
      case JDBC_PASSWORD => resource.azureResource.jdbcPassword
      case JDBC_DRIVER => resource.azureResource.jdbcDriver
      case JDBC_AUTH_FLAG => resource.azureResource.jdbcAuthFlag
      case JDBC_TOKEN => resource.azureResource.jdbcToken
      case _ => EMPTY_STRING
    }
  }
}

private[feathr] object SQLResourceInfoSetter{
  val sqlSetter = new SQLResourceInfoSetter()

  def setup(sparkConf: SparkConf, config: DataSourceConfig, resource: Resource): Unit ={
    if (config.configStr.isDefined){
      sqlSetter.setupSparkConf(sparkConf, Some(config), Some(resource))
    }
  }
}