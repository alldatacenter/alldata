package com.linkedin.feathr.offline.config.datasource

import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.SparkSession

/**
 * Base class to implement different data source configuration extraction.
 * It Contains:
 * 1. setup() combination of setupSparkConfig and setupProperties
 * 2. setupSparkConfig() updates spark session configuration with authentication parameters.
 * 3. setupProperties() merge spark read options into properties.
 * 4. getAuthStr() get Authentication string from job context or local configs
 * 5. getAuthFromContext() get Authentication from job context
 * 6. getAuthFromConfig() get Authentication from local configs
 * When A new Data source is enabled, Please Follow the Steps Below:
 * Step 1: Add a new ResourceInfoSetter Class in current package
 * Step 2: Update DataSourceConfigs Class based on input params (Job Param and Python Client)
 * Step 3: Update getConfigs() and related Setup Functions in DataSourceConfigUtils
 * Step 4: Update related Jobs with new Setup Functions
 */
private[feathr] abstract class ResourceInfoSetter() {
  val EMPTY_STRING = ""
  val params: List[String]

  @transient lazy val logger = LogManager.getLogger(getClass.getName)

  def setup(ss: SparkSession, context: DataSourceConfig, resource: Resource): Unit = {
    setupHadoopConfig(ss, Some(context), Some(resource))
  }

  def setupHadoopConfig(ss: SparkSession, context: Option[DataSourceConfig] = None, resource: Option[Resource] = None): Unit = {}

  def getAuthStr(str: String, context: Option[DataSourceConfig] = None, resource: Option[Resource] = None): String = {
    sys.env.getOrElse(str, if (context.isDefined) {
      getAuthFromContext(str, context.get)
    } else if (resource.isDefined) {
      getAuthFromConfig(str, resource.get)
    } else EMPTY_STRING)
  }

  def getAuthFromContext(str: String, context: DataSourceConfig): String = {
    context.config.map(config => {
      if (config.hasPath(str)) {
        config.getString(str)
      } else {
        logger.warn(f"$str doesn't exist in the config so it's not set")
        EMPTY_STRING
      }
    }).getOrElse(EMPTY_STRING)
  }

  def getAuthFromConfig(str: String, resource: Resource): String
}