package com.linkedin.feathr.offline

import com.linkedin.feathr.common.TaggedFeatureName
import com.linkedin.feathr.offline.job.{LocalFeatureGenJob, LocalFeatureJoinJob}
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.util.{FeathrTestUtils, SparkFeaturizedDataset}
import org.apache.hadoop.conf.Configuration
import org.apache.spark.sql.SparkSession

// This class is mainly used to create a spark session for all the feathr integ tests.
abstract class FeathrIntegTest extends TestFeathr {

  final val SPARK_DEFAULT_PARALLELISM: String = "4"
  final val SPARK_SQL_SHUFFLE_PARTITIONS: String = SPARK_DEFAULT_PARALLELISM
  protected var hadoopConf: Configuration = _

  val generatedDataFolder = "src/integTest/generated"
  val mockDataFolder = generatedDataFolder + "/mockData"
  val trainingData = "src/test/resources/obs/"
  val featureData = mockDataFolder + "/a_feature_data"

  /**
   * Wrapper method for localFeatureJoinJob.localFeatureJoinAsDF to be used by all integration tests.
   *
   * @param joinConfigAsString      join config as a string
   * @param featureDefAsString      feature def config as a string
   * @param observationDataPath     path to the observation data
   * @param extraParams             addition join parameters
   * @return Joined SparkFeaturizedDataset
   */
  private[offline] def runLocalFeatureJoinForTest(
      joinConfigAsString: String,
      featureDefAsString: String,
      observationDataPath: String,
      extraParams: Array[String] = Array(),
      mvelContext: Option[FeathrExpressionExecutionContext] = None): SparkFeaturizedDataset = {
    LocalFeatureJoinJob.joinWithHoconJoinConfig(joinConfigAsString, featureDefAsString, observationDataPath, extraParams, dataPathHandlers=List(), mvelContext=mvelContext)
  }

  def getOrCreateSparkSession: SparkSession = {
    if (ss == null) {
      ss = constructSparkSession()
    }
    ss
  }

  def getOrCreateConf: Configuration = {
    if (hadoopConf == null) {
      getOrCreateSparkSession
      hadoopConf = ss.sparkContext.hadoopConfiguration
    }
    hadoopConf
  }

  private def constructSparkSession() = {
    FeathrTestUtils.getSparkSession()
  }

  /**
   * Wrapper method for localFeatureGenJob.localFeatureGenerate to be used by all integration tests.
   *
   * @param featureGenConfigStr feature generation config string
   * @param featureDefAsString  feature definition config string
   * @return map from the taggedFeatureName to the corresponding SparkFeaturizedDataset
   */
  private[offline] def localFeatureGenerate(featureGenConfigStr: String, featureDefAsString: String): Map[TaggedFeatureName, SparkFeaturizedDataset] = {
    LocalFeatureGenJob.localFeatureGenerate(featureGenConfigStr, featureDefAsString, List())
  }

  /**
   * Wrapper method for localFeatureGenJob.localFeatureGenerate to be used by all integration tests.
   *
   * @param featureGenConfigStr sequence of eature generation config string
   * @param featureDefAsString  feature definition config string
   * @return map from the taggedFeatureName to the corresponding SparkFeaturizedDataset
   */
  private[offline] def localFeatureGenerateForSeqOfFeatures(
      featureGenConfigStr: Seq[String],
      featureDefAsString: String): Map[TaggedFeatureName, SparkFeaturizedDataset] = {
    LocalFeatureGenJob.localFeatureGenerate(featureGenConfigStr, featureDefAsString, List())
  }

}
