package com.linkedin.feathr.offline.config.datasource

import com.linkedin.feathr.offline.config.datasource.KafkaResourceInfoSetter.SASL_JAAS_CONFIG
import org.apache.spark.sql.SparkSession

/**
 * Class to setup Kafka authentication
 */
private[feathr]  class KafkaResourceInfoSetter extends ResourceInfoSetter() {

  override val params = List(SASL_JAAS_CONFIG)

  def setupSparkStreamingConf(ss: SparkSession, context: Option[DataSourceConfig], resource: Option[Resource]): Unit = {
    val sasl = getAuthStr(SASL_JAAS_CONFIG, context, resource)
    ss.conf.set(SASL_JAAS_CONFIG, sasl)
  }

  def getAuthFromConfig(str: String, resource: Resource): String = ???
}

private[feathr] object KafkaResourceInfoSetter{
  val kafkaSetter = new KafkaResourceInfoSetter()
  val SASL_JAAS_CONFIG = "KAFKA_SASL_JAAS_CONFIG"
  def setup(ss: SparkSession, config: DataSourceConfig, resource: Resource): Unit ={
    if (config.configStr.isDefined){
      kafkaSetter.setupSparkStreamingConf(ss, Some(config), Some(resource))
    }
  }
}


