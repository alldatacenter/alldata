package com.linkedin.feathr.offline.source.pathutil

import com.linkedin.feathr.common.DateTimeResolution
import com.linkedin.feathr.offline.{TestFeathr, TestUtils}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * unit test for TimeBasedPathGenerator
 */
class TestTimeBasedHdfsPathGenerator extends TestFeathr with MockitoSugar{

  @Test(description = "test generate daily file list")
  def testGenerateDailyFiles() : Unit = {
    val mockPathChecker = mock[PathChecker]
    val pathGenerator = new TimeBasedHdfsPathGenerator(mockPathChecker)

    val pathInfo = PathInfo("src/test/resources/generation/daily/", DateTimeResolution.DAILY, "yyyy/MM/dd")
    val interval = TestUtils.createDailyInterval("2019-05-18", "2019-05-20")
    val pathList = pathGenerator.generate(pathInfo, interval, false)
    assertEquals(pathList.toList, List("src/test/resources/generation/daily/2019/05/18", "src/test/resources/generation/daily/2019/05/19"))
    verifyNoMoreInteractions(mockPathChecker)
  }

  @Test(description = "test generate hourly file list")
  def testGenerateHourlyFiles() : Unit = {
    val mockPathChecker = mock[PathChecker]
    val pathGenerator = new TimeBasedHdfsPathGenerator(mockPathChecker)

    val pathInfo = PathInfo("src/test/resources/generationHourly/hourly/", DateTimeResolution.HOURLY, "yyyy/MM/dd/HH")
    val interval = TestUtils.createHourlyInterval("2019-05-19-01", "2019-05-19-03")
    val pathList = pathGenerator.generate(pathInfo, interval, false)
    assertEquals(pathList.toList, List("src/test/resources/generationHourly/hourly/2019/05/19/01", "src/test/resources/generationHourly/hourly/2019/05/19/02"))
    verifyNoMoreInteractions(mockPathChecker)
  }

  @Test(description = "test generate date partition file list")
  def testGenerateDatePartitionFileList() : Unit = {
    val mockPathChecker = mock[PathChecker]
    val pathGenerator = new TimeBasedHdfsPathGenerator(mockPathChecker)

    val pathInfo = PathInfo("src/test/resources/datePartitionSource/datepartition=", DateTimeResolution.DAILY, "yyyy-MM-dd-00")
    val interval = TestUtils.createDailyInterval("2019-05-18", "2019-05-20")
    val pathList = pathGenerator.generate(pathInfo, interval, false)
    assertEquals(pathList.toList, List("src/test/resources/datePartitionSource/datepartition=2019-05-18-00",
      "src/test/resources/datePartitionSource/datepartition=2019-05-19-00"))
    verifyNoMoreInteractions(mockPathChecker)
  }

  @Test(description = "test ignore missing files")
  def testGenerateDailyFilesWithoutMissingFiles() : Unit = {
    val mockPathChecker = mock[PathChecker]
    val pathGenerator = new TimeBasedHdfsPathGenerator(mockPathChecker)

    val pathInfo = PathInfo("src/test/resources/generation/daily/", DateTimeResolution.DAILY, "yyyy/MM/dd")
    when(mockPathChecker.exists("src/test/resources/generation/daily/2019/05/18")).thenReturn(false)
    when(mockPathChecker.exists("src/test/resources/generation/daily/2019/05/19")).thenReturn(true)
    val interval = TestUtils.createDailyInterval("2019-05-18", "2019-05-20")
    val pathList = pathGenerator.generate(pathInfo, interval, true)
    assertEquals(pathList.toList, List("src/test/resources/generation/daily/2019/05/19"))
    verify(mockPathChecker).exists("src/test/resources/generation/daily/2019/05/18")
    verify(mockPathChecker).exists("src/test/resources/generation/daily/2019/05/19")
    verifyNoMoreInteractions(mockPathChecker)
  }

  @Test(description = "test generate daily file list with hour truncated")
  def testGenerateDailyFilesWithHourTruncated() : Unit = {
    val mockPathChecker = mock[PathChecker]
    val pathGenerator = new TimeBasedHdfsPathGenerator(mockPathChecker)

    val pathInfo = PathInfo("src/test/resources/generation/daily/", DateTimeResolution.DAILY, "yyyy/MM/dd")
    val interval = TestUtils.createHourlyInterval("2019-05-18-05", "2019-05-19-08")
    val pathList = pathGenerator.generate(pathInfo, interval, false)
    assertEquals(pathList.toList, List("src/test/resources/generation/daily/2019/05/18", "src/test/resources/generation/daily/2019/05/19"))
    verifyNoMoreInteractions(mockPathChecker)
  }
}
