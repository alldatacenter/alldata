package com.linkedin.feathr.offline.testfwk.generation

import com.linkedin.feathr.common.TaggedFeatureName
import com.linkedin.feathr.offline.client.FeathrClient
import com.linkedin.feathr.offline.config.FeathrConfigLoader
import com.linkedin.feathr.offline.generation.FeatureGenKeyTagAnalyzer
import com.linkedin.feathr.offline.job.{FeatureGenConfigOverrider, FeatureGenJobContext, FeatureGenSpec, LocalFeatureGenJob}
import com.linkedin.feathr.offline.source.DataSource
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler
import com.linkedin.feathr.offline.testfwk.{FeatureDefMockContext, SourceMockParam, TestFwkUtils}
import com.linkedin.feathr.offline.util.{FeathrTestUtils, SparkFeaturizedDataset}
import com.typesafe.config.ConfigFactory
import org.apache.logging.log4j.LogManager
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import java.util.Optional
import java.nio.file.Paths
import java.nio.file.Files
import scala.collection.JavaConverters._

/**
 * Component to test Feathr generation
 * @param resourceLocation input source paths
 */
class FeathrGenTestComponent(resourceLocation: Map[String, List[String]], dataPathHandlers: List[DataPathHandler], extraParams: Array[String] = Array()) {
  val ss = FeathrTestUtils.getSparkSession()
  private val logger = LogManager.getLogger(getClass)

  /**
   * run test case with resource [[resourceLocation]]
   * @param inputConstraintOpt mock data constraint config
   * @param outputValidator validation class to validate test output
   * @return test result
   */
  def run(mockDataDir: String, inputConstraintOpt: Optional[String]): Map[TaggedFeatureName, SparkFeaturizedDataset] = {
    // set mockdata dir
    ss.conf.set("mockdata_dir", mockDataDir)
    val dataConfiguration = discoverDataConfigurations()
    val rewrittenConfiguration: FeatureGenDataConfigurationWithMockContext = rewriteConfiguration(dataConfiguration, inputConstraintOpt, None)
    val res = evaluate(rewrittenConfiguration)
    res
  }

  /**
   * Validate the feature config and some basic information.
   * @return Tuple of: true(validation passed) or false(validation failed) to error message.
   */
  def validate(featureName: String, mockDataDir: String): (Boolean, String) = {
    val overriddenFeatureDefConfig = getOverriddenFeatureDefConfig()

    val feathrConfigLoader = FeathrConfigLoader();
    val feathrConfig = feathrConfigLoader.load(overriddenFeatureDefConfig.get)

    val allFeatures =  feathrConfig.anchoredFeatures.keySet union feathrConfig.derivedFeatures.keySet
    if (!allFeatures.contains(featureName)) {
      val localFeatureDefConfigPaths = resourceLocation.get(FeathrGenTestComponent.LocalConfPaths)
      val errorMsg = f"${Console.RED}Error: feature $featureName is not defined in your config: ${localFeatureDefConfigPaths.get.mkString(",")}${Console.RESET}"
      println(errorMsg)
      return (false, errorMsg)
    }

    if (feathrConfig.anchoredFeatures.contains(featureName)) {
      val sourceInConfig = feathrConfig.anchoredFeatures(featureName).source

      // get the mock dir from the config dir
      val localTestDir = Paths.get(mockDataDir, sourceInConfig.path.replace("abfss://", ""))

      val exist = Files.exists(localTestDir)
      println(f"${Console.GREEN}Your source is specified as: {$sourceInConfig}${Console.RESET}")
      if (exist) {
        println(f"${Console.GREEN}Local mock source file exist: {$localTestDir}${Console.RESET}")
      } else if (sourceInConfig.path.startsWith("jdbc:")) {
        println(f"${Console.GREEN}Local mock source file doesn't exist try local JDBC: {$localTestDir}${Console.RESET}")
      } else {
        val errorMsg = f"${Console.RED}Error: Local mock source file doesn't exist: $localTestDir${Console.RESET}"
        println(errorMsg)
        return (false, errorMsg)
      }
    }
    (true, "")
  }

  private def getOverriddenFeatureDefConfig(): Option[String] = {
    val defaultParams = Array("--work-dir", "featureGen/localFeatureGenerate/")
    val jobContext = FeatureGenJobContext.parse(defaultParams ++ extraParams)
    // find local feature config
    val localFeatureDefConfigPaths = resourceLocation.get(FeathrGenTestComponent.LocalConfPaths)
    val localFeatureDefConfig = localFeatureDefConfigPaths.map(paths => TestFwkUtils.readLocalConfFileAsString(paths.mkString(",")))
    val featureMPDefConfigStr = TestFwkUtils.getFeathrConfFromFeatureRepo()
    val featureDefConfigs = (localFeatureDefConfig ++ featureMPDefConfigStr).reduceOption(_ + "\n" + _)
    val (overriddenFeatureDefConfig, _) = FeatureGenConfigOverrider.overrideFeatureDefs(featureDefConfigs, None, jobContext)
    overriddenFeatureDefConfig
  }

  /**
   * based on resourceLocation, discover Feathr data configs, e.g. Feature configs, join config, observation path, etc.
   * @return discovered config
   */
  private def discoverDataConfigurations(): FeatureGenDataConfiguration = {
    val defaultParams = Array("--work-dir", "featureGen/localFeatureGenerate/")
    val jobContext = FeatureGenJobContext.parse(defaultParams ++ extraParams)
    val overriddenFeatureDefConfig = getOverriddenFeatureDefConfig()
    val feathrClient = FeathrClient.builder(ss)
    .addLocalOverrideDef(overriddenFeatureDefConfig)
    .addDataPathHandlers(dataPathHandlers)
    .build()
    // find generation config
    val genConfigAsString = if (resourceLocation.contains(FeathrGenTestComponent.GenerationConfigPath)) {
      val genConfigPath = resourceLocation.get(FeathrGenTestComponent.GenerationConfigPath).map(_.mkString(",")).get
      TestFwkUtils.readLocalConfFileAsString(genConfigPath)
    } else {
      resourceLocation(FeathrGenTestComponent.LocalGenConfString).head
    }

    val withParamsOverrideConfigStr = FeatureGenConfigOverrider.applyOverride(genConfigAsString, jobContext.paramsOverride)
    val dataLoaderHandlers: List[DataLoaderHandler] = dataPathHandlers.map(_.dataLoaderHandler)
    val featureGenSpec = FeatureGenSpec.parse(genConfigAsString, jobContext, dataLoaderHandlers)
    // find feature sources
    val featureSources = getAllFeatureSources(feathrClient, featureGenSpec)
    new FeatureGenDataConfiguration(featureSources, withParamsOverrideConfigStr, overriddenFeatureDefConfig, featureGenSpec)
  }

  /**
   * rewrite Feathr data configuration for local run
   * @param dataConfiguration feathr data configurations (feature definition, join config, observation path, etc.)
   * @param inputConstraintOpt mock data constraint config
   * @return rewritten configuration with mock parameters
   */
  def rewriteConfiguration(
      dataConfiguration: FeatureGenDataConfiguration,
      inputConstraintOpt: Optional[String],
      mockDataBaseDir: Option[String]): FeatureGenDataConfigurationWithMockContext = {
    val rewrittenFeatureDef: String = dataConfiguration.localFeatureDefConfig.get
    val rewrittenFeatureGenDef: String = rewriteFeatureGenConfig(dataConfiguration.genConfigAsString)
    val featureSourceMockParams: List[SourceMockParam] = dataConfiguration.featureSources.map(source => {
      val featureGenSpec = dataConfiguration.featureGenSpec
      val endTime = DateTime.parse(featureGenSpec.endTimeStr, DateTimeFormat.forPattern(featureGenSpec.endTimeFormat))
      TestFwkUtils.createMockParam(source, endTime, mockDataBaseDir)
    })
    val featureDefMockContext = new FeatureDefMockContext(featureSourceMockParams, rewrittenFeatureDef)
    val dataConfigurationMockContext = new FeatureGenDataConfigurationMockContext(featureDefMockContext, rewrittenFeatureGenDef)
    new FeatureGenDataConfigurationWithMockContext(dataConfiguration, dataConfigurationMockContext)
  }

  /**
   * rewrite the feature gen config so that it can be used to execute locally
   * @param featureGenConfig original join config as a string
   * @return rewritten join config as a string
   */
  private def rewriteFeatureGenConfig(featureGenConfig: String): String = {
    // may need other rewrite later
    rewriteOutputPaths(featureGenConfig)
  }

  /**
   * Rewrite all output path to local path
   * i.e. if the output path starts with /, e.g /job/xx, rewrite to /tmp/job/xxx, since we cannot create /jobs locally
   */
  private def rewriteOutputPaths(featureGenConfigStr: String): String = {
    val fullConfig = ConfigFactory.parseString(featureGenConfigStr)
    // override user specified parameters
    // typeSafe config does not support path expression to access array elements directly
    // see https://github.com/lightbend/config/issues/30, so we need to special handle path expression for output array
    val PATH_KEY_NAME = "params.path"
    val REWRITE_BASE_PATH = "featureGen/test/output"
    val withOutputOverrideStr = fullConfig
      .getConfigList("operational.output")
      .asScala
      .zipWithIndex
      .collect {
        case (output, idx) if (output.hasPath(PATH_KEY_NAME) && output.getString(PATH_KEY_NAME).startsWith("/")) =>
          "output(" + idx + s").${PATH_KEY_NAME}:${REWRITE_BASE_PATH}" + output.getString(PATH_KEY_NAME)
      }
      .mkString(",")
    FeatureGenConfigOverrider.applyOverride(featureGenConfigStr, Some("[" + withOutputOverrideStr + "]"))
  }

  /**
   * local run Feathr join job
   * @param rewrittenConfiguration Feathr configurations
   * @return joined RDD
   */
  private def evaluate(rewrittenConfiguration: FeatureGenDataConfigurationWithMockContext): Map[TaggedFeatureName, SparkFeaturizedDataset] = {
    val res = LocalFeatureGenJob.localFeatureGenerate(
      rewrittenConfiguration.dataConfigurationMockContext.rewrittenFeatureGenDef,
      rewrittenConfiguration.dataConfiguration.localFeatureDefConfig.get,
      extraParams,
      dataPathHandlers)
    res
  }

  /**
   * get all the sources in the feature configs
   * @param feathrClient FeathrClient
   * @param featureGenSpec Feathr join config
   */
  private def getAllFeatureSources(feathrClient: FeathrClient, featureGenSpec: FeatureGenSpec): List[DataSource] = {
    val featureGroups = feathrClient.getFeatureGroups()
    val keyTaggedAnchoredFeatures = FeatureGenKeyTagAnalyzer.inferKeyTagsForAnchoredFeatures(featureGenSpec, featureGroups)
    val keyTaggedDerivedFeatures = FeatureGenKeyTagAnalyzer.inferKeyTagsForDerivedFeatures(featureGenSpec, featureGroups, keyTaggedAnchoredFeatures)
    val keyTaggedFeaturesWithDate = keyTaggedAnchoredFeatures ++ keyTaggedDerivedFeatures
    feathrClient.getAllFeatureSources(keyTaggedFeaturesWithDate)
  }
}

object FeathrGenTestComponent {
  // base path to store mock data
  private val mockDataBaseDir = "src/test/resources/mockdata/"
  val GenerationConfigPath = "generationConfigPath"
  val LocalConfPaths = "localConfPaths"
  val LocalGenConfString = "localGenConfString"

  /**
   * get the path to store mock data for a source path
   * @param path path to mock
   * @return generated path to store the mock data
   */
  def getMockPath(path: String): String = {
    FeathrGenTestComponent.mockDataBaseDir + Paths.get(path)
  }
}
