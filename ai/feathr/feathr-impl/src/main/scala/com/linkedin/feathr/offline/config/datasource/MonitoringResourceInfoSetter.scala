package com.linkedin.feathr.offline.config.datasource

import org.apache.spark.sql.SparkSession

private[feathr] class MonitoringResourceInfoSetter extends ResourceInfoSetter() {
  override val params: List[String] = List()

  override def setupHadoopConfig(ss: SparkSession, context: Option[DataSourceConfig], resource: Option[Resource]): Unit = {
    context.foreach(dataSourceConfig => {
      ss.conf.set("monitoring_database_url", getAuthFromContext("MONITORING_DATABASE_SQL_URL", dataSourceConfig))
      ss.conf.set("monitoring_database_user", getAuthFromContext("MONITORING_DATABASE_SQL_USER", dataSourceConfig))
      ss.conf.set("monitoring_database_password", getAuthFromContext("MONITORING_DATABASE_SQL_PASSWORD", dataSourceConfig))
    })
  }

  override def getAuthFromConfig(str: String, resource: Resource): String = ???
}


private[feathr] object MonitoringResourceInfoSetter{
  val monitoringSetter = new MonitoringResourceInfoSetter()

  def setup(ss: SparkSession, config: DataSourceConfig, resource: Resource): Unit ={
    monitoringSetter.setup(ss, config, resource)
  }
}