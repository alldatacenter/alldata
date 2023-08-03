package com.linkedin.feathr.offline.source.dataloader.jdbc

import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * This is used for microsoft Sql server data source JDBC connector
 * @param ss
 */
class SqlServerDataLoader(ss: SparkSession) extends JdbcConnector(ss){
  override val format = "com.microsoft.sqlserver.jdbc.spark"
}
