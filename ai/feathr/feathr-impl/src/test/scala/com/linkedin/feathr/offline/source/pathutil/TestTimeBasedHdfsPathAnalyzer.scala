package com.linkedin.feathr.offline.source.pathutil

import com.linkedin.feathr.common.DateTimeResolution
import com.linkedin.feathr.offline.TestFeathr
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * unit test for TimeBasedHdfsPathAnalyzer
 */
class TestTimeBasedHdfsPathAnalyzer extends TestFeathr with MockitoSugar {

  @Test(description = "test analyzePath with explicit daily path")
  def testAnalyzePathWithDailyPath(): Unit = {
    val mockPathChecker = mock[PathChecker]
    val pathAnalyzer = new TimeBasedHdfsPathAnalyzer(mockPathChecker, List())
    assertEquals(
      pathAnalyzer.analyze("src/test/resources/generation/daily/"),
      PathInfo("src/test/resources/generation/daily/", DateTimeResolution.DAILY, "yyyy/MM/dd"))
    verifyNoMoreInteractions(mockPathChecker)
  }

  @Test(description = "test analyzePath with explicit hourly path")
  def testAnalyzePathWithHourlyPath(): Unit = {
    val mockPathChecker = mock[PathChecker]
    val pathAnalyzer = new TimeBasedHdfsPathAnalyzer(mockPathChecker, List())
    assertEquals(
      pathAnalyzer.analyze("src/test/resources/generationHourly/hourly/"),
      PathInfo("src/test/resources/generationHourly/hourly/", DateTimeResolution.HOURLY, "yyyy/MM/dd/HH"))
    verifyNoMoreInteractions(mockPathChecker)
  }

  @Test(description = "test analyzePath by detecting daily")
  def testAnalyzePathWithImplicitDailyPath(): Unit = {
    val mockPathChecker = mock[PathChecker]
    when(mockPathChecker.exists("src/test/resources/generation/daily/")).thenReturn(true)
    val pathAnalyzer = new TimeBasedHdfsPathAnalyzer(mockPathChecker, List())
    assertEquals(
      pathAnalyzer.analyze("src/test/resources/generation"),
      PathInfo("src/test/resources/generation/daily/", DateTimeResolution.DAILY, "yyyy/MM/dd"))
    verify(mockPathChecker).exists("src/test/resources/generation/daily/")
    verifyNoMoreInteractions(mockPathChecker)
  }

  @Test(description = "test analyzePath by detecting hourly")
  def testAnalyzePathWithImplicitHourlyPath(): Unit = {
    val mockPathChecker = mock[PathChecker]
    when(mockPathChecker.exists("src/test/resources/generationHourly/hourly/")).thenReturn(true)
    val pathAnalyzer = new TimeBasedHdfsPathAnalyzer(mockPathChecker, List())
    assertEquals(
      pathAnalyzer.analyze("src/test/resources/generationHourly"),
      PathInfo("src/test/resources/generationHourly/hourly/", DateTimeResolution.HOURLY, "yyyy/MM/dd/HH"))
    verify(mockPathChecker).exists("src/test/resources/generationHourly/daily/")
    verify(mockPathChecker).exists("src/test/resources/generationHourly/hourly/")
    verifyNoMoreInteractions(mockPathChecker)
  }

  @Test(description = "test analyzePath with date partition")
  def testAnalyzePathWithDatePartition(): Unit = {
    val mockPathChecker = mock[PathChecker]
    val pathAnalyzer = new TimeBasedHdfsPathAnalyzer(mockPathChecker, List())
    assertEquals(
      pathAnalyzer.analyze("dalids://src/test/resources/datePartitionSource"),
      PathInfo("dalids://src/test/resources/datePartitionSource/", DateTimeResolution.DAILY, "yyyy/MM/dd"))
  }

  @Test(description = "test analyze with time partition pattern")
  def tetAnalyzePathWithTimePartitionPattern(): Unit = {
    val mockPathChecker = mock[PathChecker]
    val pathAnalyzer = new TimeBasedHdfsPathAnalyzer(mockPathChecker, List())
    assertEquals(
      pathAnalyzer.analyze("src/test/resources/generation/", "yyyy/MM/dd"),
      PathInfo("src/test/resources/generation/", DateTimeResolution.DAILY, "yyyy/MM/dd"))
    assertEquals(
      pathAnalyzer.analyze("src/test/resources/generation/", "yyyy/MM/dd"),
      PathInfo("src/test/resources/generation/", DateTimeResolution.DAILY, "yyyy/MM/dd"))
    assertEquals(
      pathAnalyzer.analyze("src/test/resources/generation/", "yyyy/MM/dd/HH"),
      PathInfo("src/test/resources/generation/", DateTimeResolution.HOURLY, "yyyy/MM/dd/HH"))
    assertEquals(
      pathAnalyzer.analyze("src/test/resources/generation/datepartition=", "yyyy-MM-dd-00"),
      PathInfo("src/test/resources/generation/datepartition=", DateTimeResolution.DAILY, "yyyy-MM-dd-00"))
  }
}
