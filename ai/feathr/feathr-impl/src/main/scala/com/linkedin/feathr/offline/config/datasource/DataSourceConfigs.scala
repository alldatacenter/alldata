package com.linkedin.feathr.offline.config.datasource

/**
 * A base class to cluster all data source related configs
 * Add ConfigStr options in this class when new data source is enabled.
 *
 * @param redisConfigStr
 * @param s3ConfigStr
 * @param adlsConfigStr
 * @param blobConfigStr
 * @param sqlConfigStr
 */
class DataSourceConfigs(
                         val redisConfigStr: Option[String] = None,
                         val s3ConfigStr: Option[String] = None,
                         val adlsConfigStr: Option[String] = None,
                         val blobConfigStr: Option[String] = None,
                         val sqlConfigStr: Option[String] = None,
                         val snowflakeConfigStr: Option[String] = None,
                         val monitoringConfigStr: Option[String] = None,
                         val kafkaConfigStr: Option[String] = None
                       ) {
  val redisConfig: DataSourceConfig = parseConfigStr(redisConfigStr)
  val s3Config: DataSourceConfig = parseConfigStr(s3ConfigStr)
  val adlsConfig: DataSourceConfig = parseConfigStr(adlsConfigStr)
  val blobConfig: DataSourceConfig = parseConfigStr(blobConfigStr)
  val sqlConfig: DataSourceConfig = parseConfigStr(sqlConfigStr)
  val snowflakeConfig: DataSourceConfig = parseConfigStr(snowflakeConfigStr)
  val monitoringConfig: DataSourceConfig = parseConfigStr(monitoringConfigStr)
  val kafkaConfig: DataSourceConfig = parseConfigStr(kafkaConfigStr)

  def parseConfigStr(configStr: Option[String] = None): DataSourceConfig = {
    new DataSourceConfig(configStr)
  }
}