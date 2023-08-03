package com.linkedin.feathr.offline.config.datasource

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.linkedin.feathr.offline.util.CmdLineParser
import org.apache.logging.log4j.{LogManager, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

import scala.io.Source

object DataSourceConfigUtils {
  val logger: Logger = LogManager.getLogger(getClass)

  private val yamlMapper = new ObjectMapper(new YAMLFactory())
  val featureStoreConfig: FeathrStoreConfig = loadYamlConfig("feathr_project/data/feathr_user_workspace/feathr_config.yaml")

  private def loadYamlConfig(yamlPath: String) = {
    try {
      val contents = Source.fromFile(yamlPath).mkString
      yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      val config = yamlMapper.readValue(contents, classOf[FeathrStoreConfig])
      config
    } catch {
      case _: Throwable =>
        FeathrStoreConfig(null)
    }
  }

  private[feathr] def getConfigs(cmdParser: CmdLineParser): DataSourceConfigs ={
    new DataSourceConfigs(
      redisConfigStr = cmdParser.extractOptionalValue("redis-config"),
      s3ConfigStr = cmdParser.extractOptionalValue("s3-config"),
      adlsConfigStr = cmdParser.extractOptionalValue("adls-config"),
      blobConfigStr = cmdParser.extractOptionalValue("blob-config"),
      sqlConfigStr = cmdParser.extractOptionalValue("sql-config"),
      snowflakeConfigStr = cmdParser.extractOptionalValue("snowflake-config"),
      monitoringConfigStr = cmdParser.extractOptionalValue("monitoring-config"),
      kafkaConfigStr = cmdParser.extractOptionalValue("kafka-config")
    )
  }

  private[feathr] def setupHadoopConf(ss:SparkSession , configs: DataSourceConfigs): Unit ={
    val resource = featureStoreConfig.resource
    ADLSResourceInfoSetter.setup(ss, configs.adlsConfig, resource)
    BlobResourceInfoSetter.setup(ss, configs.blobConfig, resource)
    S3ResourceInfoSetter.setup(ss, configs.s3Config, resource)
    SnowflakeResourceInfoSetter.setup(ss, configs.snowflakeConfig, resource)
    MonitoringResourceInfoSetter.setup(ss, configs.monitoringConfig, resource)
    KafkaResourceInfoSetter.setup(ss, configs.kafkaConfig, resource)
  }

  private[feathr] def setupSparkConf(sparkConf: SparkConf, configs: DataSourceConfigs): Unit ={
    val resource = featureStoreConfig.resource
    RedisResourceInfoSetter.setup(sparkConf, configs.redisConfig, resource)
    // TODO Re-enable this after JDBC is supported.
    // SQLResourceInfoSetter.setup(sparkConf, configs.sqlConfig, resource)
  }
}