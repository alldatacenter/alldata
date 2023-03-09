package com.linkedin.feathr.offline.job

import com.linkedin.feathr.offline.config.FeathrConfigLoader
import com.typesafe.config.{ConfigFactory, ConfigRenderOptions}

import scala.collection.JavaConverters._

/**
 * Object which contains methods to override the feature def and feature gen configs with the local configs.
 */
private[offline] object FeatureGenConfigOverrider {

  private val feathrConfigLoader = FeathrConfigLoader()

  /**
   * Construct feature gen config string by considering the override params string
   * @param featureGenConfigStr The passed in featureGenConfig string
   * @param paramsOverride  params override string
   * @return  A new featureGenConfigStr formed by replacing the overriden configs in the feature gen config
   */
  def applyOverride(featureGenConfigStr: String, paramsOverride: Option[String]): String = {
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
   * Override feature def configs with a local feature def config. The local config will override the feature def configs if
   * provided.
   * @param featureConfig feature def config
   * @param localFeatureConfig  local feature def config
   * @param jobContext  [[FeatureGenJobContext]]
   * @return
   */
  private[feathr] def overrideFeatureDefs(featureConfig: Option[String], localFeatureConfig: Option[String], jobContext: FeatureGenJobContext) = {
    val featureConfigWithOverride = if (featureConfig.isDefined && jobContext.featureConfOverride.isDefined) {
      Some(feathrConfigLoader.resolveOverride(featureConfig.get, jobContext.featureConfOverride.get))
    } else {
      featureConfig
    }
    val localFeatureConfigWithOverride = if (localFeatureConfig.isDefined && jobContext.featureConfOverride.isDefined) {
      Some(feathrConfigLoader.resolveOverride(localFeatureConfig.get, jobContext.featureConfOverride.get))
    } else {
      localFeatureConfig
    }
    (featureConfigWithOverride, localFeatureConfigWithOverride)
  }
}
