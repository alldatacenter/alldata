package com.linkedin.feathr.offline.source.dataloader.jdbc

import org.apache.spark.sql.SparkSession

sealed trait JdbcConnectorChooser

/**
 * Each Sql Type represents a SQL data source
 * When enabling new JDBC data source:
 * 1. A new SqlDbType will be added.
 * 2. Update getType tp identify this JDBC source
 * 3. The JDBC driver needs to be checkin in SBT dependencies
 * 4. A specific DataLoader needs to be added to specify the driver or add custom logics
 */
object JdbcConnectorChooser {
  case object SqlServer extends JdbcConnectorChooser
  case object Postgres extends JdbcConnectorChooser
  case object DefaultJDBC extends JdbcConnectorChooser

  def getType (url: String): JdbcConnectorChooser = url match {
    case url if url.startsWith("jdbc:sqlserver") => SqlServer
    case url if url.startsWith("jdbc:postgresql:") => Postgres
    case _ => DefaultJDBC
  }

  def getJdbcConnector(ss: SparkSession, url: String): JdbcConnector = {
    val sqlDbType = getType(url)
    val dataLoader = sqlDbType match {
      case SqlServer => new SqlServerDataLoader(ss)
      case _ => new SqlServerDataLoader(ss) //default jdbc data loader place holder
    }
    dataLoader
  }
}
