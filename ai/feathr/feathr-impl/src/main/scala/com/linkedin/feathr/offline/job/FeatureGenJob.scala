package com.linkedin.feathr.offline.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.linkedin.feathr.common.TaggedFeatureName
import com.linkedin.feathr.common.configObj.configbuilder.FeatureGenConfigBuilder
import com.linkedin.feathr.offline.client.FeathrClient
import com.linkedin.feathr.offline.config.FeathrConfigLoader
import com.linkedin.feathr.offline.config.datasource.{DataSourceConfigUtils, DataSourceConfigs}
import com.linkedin.feathr.offline.job.FeatureJoinJob._
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler
import com.linkedin.feathr.offline.transformation.AnchorToDataSourceMapper
import com.linkedin.feathr.offline.util.{CmdLineParser, FeathrUtils, OptionParam, SparkFeaturizedDataset}
import com.typesafe.config.{ConfigFactory, ConfigRenderOptions}
import org.apache.avro.generic.GenericRecord
import org.apache.commons.cli.{Option => CmdOption}
import org.apache.logging.log4j.{LogManager, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.collection.JavaConverters._
import scala.collection.mutable

object FeatureGenJob {

  type ApplicationConfigPath = String
  val logger: Logger = LogManager.getLogger(getClass)
  /**
   * Parse command line arguments, which includes application config,
   * Feathr feature definition configs and other settings
   *
   * @param args command line arguments
   * @return (applicationConfigPath, feature defintions and FeatureGenJobContext)
   * Results wil be used to construct FeathrFeatureGenJobContext
   */
  private[feathr] def parseInputArguments(args: Array[String]): (ApplicationConfigPath, FeatureDefinitionsInput, FeatureGenJobContext, DataSourceConfigs) = {
    val params = Map(
      // option long name, short name, description, arg name (null means not argument), default value (null means required)
      "feathr-config" -> OptionParam("lf", "Path of the feathr local config file", "FCONF", ""),
      "feature-config" -> OptionParam("f", "Names of the feathr feature config files", "EFCONF", ""),
      "local-override-all" -> OptionParam("loa", "Local config overrides all other configs", "LOCAL_OVERRIDE", "true"),
      "work-dir" -> OptionParam ("wd", "work directory, used to store temporary results, etc.", "WORK_DIR", ""),
      "generation-config" -> OptionParam("gc", "Path of the feature generation config file", "JCONF", null),
      "params-override" -> OptionParam("ac", "parameter to override in feature generation config", "PARAM_OVERRIDE", "[]"),
      "feature-conf-override" -> OptionParam("fco", "parameter to override in feature definition config", "FEATURE_CONF_OVERRIDE", "[]"),
      "redis-config" -> OptionParam("ac", "Authentication config for Redis", "REDIS_CONFIG", ""),
      "s3-config" -> OptionParam("sc", "Authentication config for S3", "S3_CONFIG", ""),
      "adls-config" -> OptionParam("adlc", "Authentication config for ADLS (abfs)", "ADLS_CONFIG", ""),
      "blob-config" -> OptionParam("bc", "Authentication config for Azure Blob Storage (wasb)", "BLOB_CONFIG", ""),
      "sql-config" -> OptionParam("sqlc", "Authentication config for Azure SQL Database (jdbc)", "SQL_CONFIG", ""),
      "snowflake-config" -> OptionParam("sfc", "Authentication config for Snowflake Database (jdbc)", "SNOWFLAKE_CONFIG", ""),
      "monitoring-config" -> OptionParam("mc", "Feature monitoring related configs", "MONITORING_CONFIG", ""),
      "kafka-config" -> OptionParam("kc", "Authentication config for Kafka", "KAFKA_CONFIG", ""),
      "system-properties" -> OptionParam("sps", "Additional System Properties", "SYSTEM_PROPERTIES_CONFIG", "")
    )
    val extraOptions = List(new CmdOption("LOCALMODE", "local-mode", false, "Run in local mode"))

    val cmdParser = new CmdLineParser(args, params, extraOptions)

    // Set system properties passed via arguments
    val sps = cmdParser.extractOptionalValue("system-properties").getOrElse("{}")
    val props = (new ObjectMapper()).registerModule(DefaultScalaModule).readValue(sps, classOf[mutable.HashMap[String, String]])
    props.foreach(e => scala.util.Properties.setProp(e._1, e._2))

    val applicationConfigPath = cmdParser.extractRequiredValue("generation-config")
    val featureDefinitionsInput = new FeatureDefinitionsInput(
      cmdParser.extractOptionalValue("feathr-config"),
      cmdParser.extractOptionalValue("feature-config"),
      cmdParser.extractRequiredValue("local-override-all"))
    val paramsOverride = cmdParser.extractOptionalValue("params-override")
    val featureConfOverride = cmdParser.extractOptionalValue("feature-conf-override").map(convertToHoconConfig)
    val workDir = cmdParser.extractRequiredValue("work-dir")

    val dataSourceConfigs = DataSourceConfigUtils.getConfigs(cmdParser)
    val featureGenJobContext = new FeatureGenJobContext(workDir, paramsOverride, featureConfOverride)

    println("dataSourceConfigs: ")
    println(dataSourceConfigs)
    (applicationConfigPath, featureDefinitionsInput, featureGenJobContext, dataSourceConfigs)
  }

  // Convert parameters passed from hadoop template into global vars section for feature conf
  def convertToHoconConfig(params: String): String = {
    params.stripPrefix("[").stripSuffix("]")
  }

  /**
   * generate Feathr features according to config file paths and jobContext
   * @param ss spark session
   * @param featureGenConfigPath  feature generation config file path
   * @param featureDefInputs contains feature definition file paths and settings
   * @param jobContext job context
   * @return generated feature data
   */
  def run(
      ss: SparkSession,
      featureGenConfigPath: String,
      featureDefInputs: FeatureDefinitionsInput,
      jobContext: FeatureGenJobContext):  Map[TaggedFeatureName, SparkFeaturizedDataset] = {
    // load feature definitions input files
    val applicationConfig = hdfsFileReader(ss, featureGenConfigPath)
    val featureConfig = featureDefInputs.feathrFeatureDefPaths.map(path => hdfsFileReader(ss, path))
    val localFeatureConfig = featureDefInputs.feathrLocalFeatureDefPath.map(path => hdfsFileReader(ss, path))
    val (featureConfigWithOverride, localFeatureConfigWithOverride) = overrideFeatureDefs(featureConfig, localFeatureConfig, jobContext)
    run(ss, applicationConfig, featureConfigWithOverride, localFeatureConfigWithOverride, jobContext)
  }

  private[feathr] def overrideFeatureDefs(featureConfig: Option[String], localFeatureConfig: Option[String], jobContext: FeatureGenJobContext) = {
    val featureConfigWithOverride = if (featureConfig.isDefined && jobContext.featureConfOverride.isDefined) {
      Some(FeathrConfigLoader.resolveOverride(featureConfig.get, jobContext.featureConfOverride.get))
    } else {
      featureConfig
    }
    val localFeatureConfigWithOverride = if (localFeatureConfig.isDefined && jobContext.featureConfOverride.isDefined) {
      Some(FeathrConfigLoader.resolveOverride(localFeatureConfig.get, jobContext.featureConfOverride.get))
    } else {
      localFeatureConfig
    }
    (featureConfigWithOverride, localFeatureConfigWithOverride)
  }

  /**
   * generate Feathr features according to config file contents and jobContext
   * @param ss spark session
   * @param featureGenConfig feature generation config as a string
   * @param featureDefConfig feature definition config, comes from feature repo
   * @param localFeatureConfig feature definition config, comes from local feature definition files
   * @param jobContext job context
   * @return generated feature data
   */
  private[feathr] def run(
      ss: SparkSession,
      featureGenConfig: String,
      featureDefConfig: Option[String],
      localFeatureConfig: Option[String],
      jobContext: FeatureGenJobContext,
      dataPathHandlers: List[DataPathHandler]=List()): Map[TaggedFeatureName, SparkFeaturizedDataset] = {

    logger.info(s"featureDefConfig : ${featureDefConfig}")
    logger.info(s"localFeatureConfig : ${localFeatureConfig}")
    val feathrClient =
        FeathrClient.builder(ss)
          .addFeatureDef(featureDefConfig)
          .addLocalOverrideDef(localFeatureConfig)
          .addDataPathHandlers(dataPathHandlers)
          .build()
    val featureGenSpec = parseFeatureGenApplicationConfig(featureGenConfig, jobContext, dataPathHandlers)
    feathrClient.generateFeatures(featureGenSpec)
  }

  /**
   * parsing feature generation config string to [[FeatureGenSpec]]
   * @param featureGenConfigStr feature generation config as a string
   * @param featureGenJobContext feature generation context
   * @return Feature generation Specifications
   */
  private[feathr] def parseFeatureGenApplicationConfig(featureGenConfigStr: String, featureGenJobContext: FeatureGenJobContext,
      dataPathHandlers: List[DataPathHandler]=List()): FeatureGenSpec = {
    val withParamsOverrideConfigStr = overrideFeatureGeneration(featureGenConfigStr, featureGenJobContext.paramsOverride)
    val withParamsOverrideConfig = ConfigFactory.parseString(withParamsOverrideConfigStr)
    val featureGenConfig = FeatureGenConfigBuilder.build(withParamsOverrideConfig)
    val dataLoaderHandlers: List[DataLoaderHandler] = dataPathHandlers.map(_.dataLoaderHandler)
    new FeatureGenSpec(featureGenConfig, dataLoaderHandlers)
  }

  private[feathr] def overrideFeatureGeneration(featureGenConfigStr: String, paramsOverride: Option[String]): String = {
    val fullConfig = ConfigFactory.parseString(featureGenConfigStr)
    val withParamsOverrideConfig = paramsOverride
      .map(configStr => {
        // override user specified parameters
        val paramOverrideConfigStr = "operational: {" + configStr.stripPrefix("[").stripSuffix("]") + "}"
        val paramOverrideConfig = ConfigFactory.parseString(paramOverrideConfigStr)
        // typeSafe config does not support path expression to access array elements directly
        // see https://github.com/lightbend/config/issues/30, so we need to special handle path expression for output array
        val withOutputOverrideStr = fullConfig
          .getConfigList("operational.output")
          .asScala
          .zipWithIndex
          .map {
            case (output, idx) =>
              val key = "operational.output(" + idx + ")"
              val withOverride = if (paramOverrideConfig.hasPath(key)) {
                paramOverrideConfig.getConfig(key).withFallback(output)
              } else output
              withOverride.root().render(ConfigRenderOptions.concise())
          }
          .mkString(",")
        val withOutputOverride = ConfigFactory.parseString("operational.output:[" + withOutputOverrideStr + "]")
        // override the config with paramOverrideConfig
        paramOverrideConfig.withFallback(withOutputOverride).withFallback(fullConfig)
      })
      .getOrElse(fullConfig)
    withParamsOverrideConfig.root().render()
  }

  /**
   * Load the DataFrames for sources that needs preprocessing by Pyspark.
   * @param args Same arguments for the main job.
   * @param featureNamesInAnchorSet A set of feature names of an anchor sorted and joined by comma. For example,
   *                                anchor1 -> f1, f2, anchor2 -> f3, f4. Then the set is ("f1,f2", "f3,f4")
   * @return A Java map whose key is Feature names of an anchor sorted and joined by comma and value is the DataFrame
   *         for this anchor source. For example, anchor1 -> f1, f2, anchor2 -> f3, f4. Then the result is
   *         Map("f1,f2" -> df1, "f3,f4" -> df2).
   */
  def loadSourceDataframe(args: Array[String], featureNamesInAnchorSet: java.util.Set[String]): java.util.Map[String, DataFrame] = {
    logger.info("FeatureJoinJob args are: " + args)
    logger.info("Feature join job: loadDataframe")
    logger.info(featureNamesInAnchorSet)
    val feathrGenPreparationInfo = prepareSparkSession(args)
    val sparkSession = feathrGenPreparationInfo.sparkSession
    val featureDefs = feathrGenPreparationInfo.featureDefs
    val jobContext = feathrGenPreparationInfo.jobContext

  val featureConfig = featureDefs.feathrFeatureDefPaths.map(path => hdfsFileReader(sparkSession, path))
      val localFeatureConfig = featureDefs.feathrLocalFeatureDefPath.map(path => hdfsFileReader(sparkSession, path))
      val (featureConfigWithOverride, localFeatureConfigWithOverride) = overrideFeatureDefs(featureConfig, localFeatureConfig, jobContext)

    //TODO: fix python errors for loadSourceDataFrame, add dataPathLoader Support
    val feathrClient =
      FeathrClient.builder(sparkSession)
        .addFeatureDef(featureConfig)
        .addLocalOverrideDef(localFeatureConfigWithOverride)
        .build()
    val allAnchoredFeatures = feathrClient.allAnchoredFeatures

    // Using AnchorToDataSourceMapper to load DataFrame for preprocessing
    val failOnMissing = FeathrUtils.getFeathrJobParam(sparkSession, FeathrUtils.FAIL_ON_MISSING_PARTITION).toBoolean
    val anchorToDataSourceMapper = new AnchorToDataSourceMapper(List()) //TODO: fix python errors for loadSourceDataFrame, add dataPathLoader Support
    val anchorsWithSource = anchorToDataSourceMapper.getBasicAnchorDFMapForJoin(
      sparkSession,
      allAnchoredFeatures.values.toSeq,
      failOnMissing)
    val updatedAnchorsWithSource = anchorsWithSource.filter(anchorEntry => anchorEntry._2.isDefined).map(anchorEntry => anchorEntry._1 -> anchorEntry._2.get)
    if (updatedAnchorsWithSource.isEmpty) return featureNamesInAnchorSet.asScala.map(featureName => featureName -> sparkSession.emptyDataFrame).toMap.asJava

    // Only load DataFrames for anchors that have preprocessing UDF
    // So we filter out anchors that doesn't have preprocessing UDFs
    // We use feature names sorted and merged as the key to find the anchor
    // For example, f1, f2 belongs to anchor. Then Map("f1,f2"-> anchor)
    updatedAnchorsWithSource
      .filter(x => featureNamesInAnchorSet.contains(x._1.featureAnchor.features.toSeq.sorted.mkString(",")))
      .map(x => (x._1.featureAnchor.features.toSeq.sorted.mkString(","), x._2.get())).asJava
  }

  def prepareSparkSession(args: Array[String]): FeathrGenPreparationInfo = {
    val (applicationConfigPath, featureDefs, jobContext, dataSourceConfigs) = parseInputArguments(args)
    val sparkConf = new SparkConf().registerKryoClasses(Array(classOf[GenericRecord]))
    DataSourceConfigUtils.setupSparkConf(sparkConf, dataSourceConfigs)
    val sparkSessionBuilder = SparkSession
      .builder()
      .config(sparkConf)
      .appName(getClass.getName)
      .enableHiveSupport()

    val ss = sparkSessionBuilder.getOrCreate()
    DataSourceConfigUtils.setupHadoopConf(ss, dataSourceConfigs)

    FeathrGenPreparationInfo(ss, applicationConfigPath, featureDefs, jobContext)
  }

  private[feathr] def process(params: Array[String]): Map[TaggedFeatureName, SparkFeaturizedDataset] = {
    val feathrGenPreparationInfo = prepareSparkSession(params)
    run(feathrGenPreparationInfo.sparkSession, feathrGenPreparationInfo.applicationConfigPath,
      feathrGenPreparationInfo.featureDefs, feathrGenPreparationInfo.jobContext)
  }

  def mainWithPreprocessedDataFrame(args: Array[String], preprocessedDfMap: java.util.Map[String, DataFrame]) {
    // Set the preprocessed DataFrame here for future usage.
    PreprocessedDataFrameManager.preprocessedDfMap = preprocessedDfMap.asScala.toMap

    main(args)
  }

  def main(args: Array[String]): Unit = {
    process(args)
  }
}

case class FeathrGenPreparationInfo(sparkSession: SparkSession, applicationConfigPath: String, featureDefs: FeatureDefinitionsInput, jobContext: FeatureGenJobContext)
