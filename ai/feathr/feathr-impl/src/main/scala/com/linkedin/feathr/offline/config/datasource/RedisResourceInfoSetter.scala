package com.linkedin.feathr.offline.config.datasource

import org.apache.spark.SparkConf

private[feathr]  class RedisResourceInfoSetter extends ResourceInfoSetter() {
  val REDIS_HOST = "REDIS_HOST"
  val REDIS_PORT = "REDIS_PORT"
  val REDIS_SSL_ENABLED = "REDIS_SSL_ENABLED"
  val REDIS_PASSWORD = "REDIS_PASSWORD"

  override val params = List(REDIS_HOST, REDIS_PORT, REDIS_SSL_ENABLED, REDIS_PASSWORD)

  def setupSparkConf(sparkConf: SparkConf, context: Option[DataSourceConfig], resource: Option[Resource]): Unit = {
    val host = getAuthStr(REDIS_HOST, context, resource)
    val port = getAuthStr(REDIS_PORT, context, resource)
    val sslEnabled = getAuthStr(REDIS_SSL_ENABLED, context, resource)
    val auth = getAuthStr(REDIS_PASSWORD, context, resource)

    sparkConf.set("spark.redis.host", host)
    sparkConf.set("spark.redis.port", port)
    sparkConf.set("spark.redis.ssl", sslEnabled)
    sparkConf.set("spark.redis.auth", auth)
  }

  def getAuthFromConfig(str: String, resource: Resource): String = {
    str match {
      case REDIS_HOST => resource.azureResource.redisHost
      case REDIS_PORT => resource.azureResource.redisPort
      case REDIS_SSL_ENABLED => resource.azureResource.redisSslEnabled
      case REDIS_PASSWORD => resource.azureResource.redisPassword
      case _ => EMPTY_STRING
    }
  }
}

private[feathr] object RedisResourceInfoSetter{
  val redisSetter = new RedisResourceInfoSetter()

  def setup(sparkConf: SparkConf, config: DataSourceConfig, resource: Resource): Unit ={
    if (config.configStr.isDefined){
      redisSetter.setupSparkConf(sparkConf, Some(config), Some(resource))
    }
  }
}
