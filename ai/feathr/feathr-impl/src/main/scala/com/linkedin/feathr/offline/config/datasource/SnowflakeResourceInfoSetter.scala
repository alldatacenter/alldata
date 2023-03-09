package com.linkedin.feathr.offline.config.datasource

import org.apache.spark.sql.SparkSession

private[feathr] class SnowflakeResourceInfoSetter extends ResourceInfoSetter() {
  override val params: List[String] = List()

  override def setupHadoopConfig(ss: SparkSession, context: Option[DataSourceConfig], resource: Option[Resource]): Unit = {
    context.foreach(dataSourceConfig => {
      ss.conf.set("sfURL", getAuthFromContext("JDBC_SF_URL", dataSourceConfig))
      ss.conf.set("sfUser", getAuthFromContext("JDBC_SF_USER", dataSourceConfig))
      ss.conf.set("sfRole", getAuthFromContext("JDBC_SF_ROLE", dataSourceConfig))
      ss.conf.set("sfWarehouse", getAuthFromContext("JDBC_SF_WAREHOUSE", dataSourceConfig))
      ss.conf.set("sfPassword", getAuthFromContext("JDBC_SF_PASSWORD", dataSourceConfig))
    })
  }

  override def getAuthFromConfig(str: String, resource: Resource): String = ???
}


private[feathr] object SnowflakeResourceInfoSetter{
  val snowflakeSetter = new SnowflakeResourceInfoSetter()

  def setup(ss: SparkSession, config: DataSourceConfig, resource: Resource): Unit ={
    snowflakeSetter.setup(ss, config, resource)
  }
}