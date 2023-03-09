package com.linkedin.feathr.offline.client

import com.linkedin.feathr.offline.TestFeathr
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * Unit tests for [[FeathrClient.Builder]]
 */
class TestFeathrClientBuilder extends TestFeathr {
  val mockFeatureDefString = Some("anchor{}")

  /**
   * Test with no parameters set.
   */
  @Test(enabled = true, expectedExceptions = Array(classOf[IllegalArgumentException]))
  def testWithBothNoParameters(): Unit = {
    FeathrClient.builder(ss).build()
  }

  /**
   * Test HDFS file reader in FeathrClient's builder class.
   */
  @Test(enabled = true)
  def testHDFSFileReader(): Unit = {
    val fileContents = FeathrClient.builder(ss).readHdfsFile(Some("src/test/resources/sampleFeatureDef.conf"))
    assertEquals(fileContents, mockFeatureDefString)
  }
}
