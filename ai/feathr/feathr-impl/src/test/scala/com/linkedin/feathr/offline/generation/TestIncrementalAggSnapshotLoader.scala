package com.linkedin.feathr.offline.generation

import com.linkedin.feathr.common.configObj.generation.OutputProcessorConfig
import com.linkedin.feathr.common.exception.FeathrConfigException
import com.linkedin.feathr.offline.job.FeatureGenSpec
import com.linkedin.feathr.offline.{FeatureName, TestFeathr}
import com.typesafe.config.Config
import org.apache.hadoop.fs.FileSystem
import org.apache.spark.sql.DataFrame
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert.{assertEquals, assertTrue}
import org.testng.annotations.Test

/**
 * Unit test class for IncrementalAggSnapshotLoader
 */
class TestIncrementalAggSnapshotLoader extends TestFeathr with MockitoSugar {

  /**
   * Test an empty incremental aggregation context is returned when incremental aggregation
   * is disabled in feature generation spec.
   */
  @Test
  def testIncrementalContextWhenIncrementalAggIsDisabled(): Unit = {
    val mockFeatureGenSpec = mock[FeatureGenSpec]
    when(mockFeatureGenSpec.isEnableIncrementalAgg()).thenReturn(false)
    val mockFileSystem = mock[FileSystem]
    val context = IncrementalAggSnapshotLoader.load(mockFeatureGenSpec, mockFileSystem, dataLoaderHandlers=List())
    assertEquals(context, IncrementalAggContext(isIncrementalAggEnabled = false, None, Map.empty[FeatureName, DataFrame], Map.empty[FeatureName, String]))
    verify(mockFeatureGenSpec).isEnableIncrementalAgg()
  }

  /**
   * Snapshot load throws exception if none of the output processors are configured for HDFS.
   */
  @Test(expectedExceptions = Array(classOf[FeathrConfigException]), expectedExceptionsMessageRegExp = ".*to support incremental aggregation, please specify.*")
  def testLoadThrowsExceptionWhenOutputConfigIsNotHDFS(): Unit = {
    val mockOutputProcessorConfig = mock[OutputProcessorConfig]
    when(mockOutputProcessorConfig.getName).thenReturn("REDIS")
    val mockFeatureGenSpec = mock[FeatureGenSpec]
    when(mockFeatureGenSpec.isEnableIncrementalAgg()).thenReturn(true)
    when(mockFeatureGenSpec.getOutputProcessorConfigs).thenReturn(Seq(mockOutputProcessorConfig))
    val mockFileSystem = mock[FileSystem]
    IncrementalAggSnapshotLoader.load(mockFeatureGenSpec, mockFileSystem, dataLoaderHandlers=List())
  }


  /**
   * Snapshot load throws exception if none of the output processors are not configured with Feature gen store name.
   */
  @Test(expectedExceptions = Array(classOf[FeathrConfigException]), expectedExceptionsMessageRegExp = ".*to support incremental aggregation, please specify.*")
  def testLoadThrowsExceptionWhenStoreNameIsNotConfigured(): Unit = {
    val mockConfig = mock[Config]
    when(mockConfig.hasPath(FeatureGenerationPathName.STORE_NAME)).thenReturn(false)
    val mockOutputProcessorConfig = mock[OutputProcessorConfig]
    when(mockOutputProcessorConfig.getName).thenReturn("HDFS")
    when(mockOutputProcessorConfig.getParams).thenReturn(mockConfig)
    val mockFeatureGenSpec = mock[FeatureGenSpec]
    when(mockFeatureGenSpec.isEnableIncrementalAgg()).thenReturn(true)
    when(mockFeatureGenSpec.getOutputProcessorConfigs).thenReturn(Seq(mockOutputProcessorConfig))
    val mockFileSystem = mock[FileSystem]
    IncrementalAggSnapshotLoader.load(mockFeatureGenSpec, mockFileSystem, dataLoaderHandlers=List())
  }

  /**
   * Snapshot load throws exception if none of the output processor configurations do not have "features".
   */
  @Test(expectedExceptions = Array(classOf[FeathrConfigException]), expectedExceptionsMessageRegExp = ".*to support incremental aggregation, please specify.*")
  def testLoadThrowsExceptionWhenFeaturesAreNotConfigured(): Unit = {
    val mockConfig = mock[Config]
    when(mockConfig.hasPath(FeatureGenerationPathName.STORE_NAME)).thenReturn(true)
    when(mockConfig.hasPath(FeatureGenerationPathName.FEATURES)).thenReturn(false)
    val mockOutputProcessorConfig = mock[OutputProcessorConfig]
    when(mockOutputProcessorConfig.getName).thenReturn("HDFS")
    when(mockOutputProcessorConfig.getParams).thenReturn(mockConfig)
    val mockFeatureGenSpec = mock[FeatureGenSpec]
    when(mockFeatureGenSpec.isEnableIncrementalAgg()).thenReturn(true)
    when(mockFeatureGenSpec.getOutputProcessorConfigs).thenReturn(Seq(mockOutputProcessorConfig))
    val mockFileSystem = mock[FileSystem]
    IncrementalAggSnapshotLoader.load(mockFeatureGenSpec, mockFileSystem, dataLoaderHandlers=List())
  }

  /**
   * Test getPreAggSnapshotMap returns empty map for the first instance of aggregation.
   */
  @Test
  def testGetPreAggSnapshotMapIsEmptyForTheFirstRun(): Unit = {
    val mockAggDirToFeatureAndDF = Seq("" -> (Seq(), None), "" -> (Seq(), None))
    assertTrue(IncrementalAggSnapshotLoader.getPreAggSnapshotMap(mockAggDirToFeatureAndDF).isEmpty)
  }
}
