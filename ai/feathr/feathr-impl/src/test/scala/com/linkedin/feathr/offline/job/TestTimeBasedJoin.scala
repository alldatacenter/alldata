package com.linkedin.feathr.offline.job

import java.text.SimpleDateFormat
import java.util.{Calendar, TimeZone}

import com.linkedin.feathr.offline.TestFeathr
import com.linkedin.feathr.offline.client.InputData
import com.linkedin.feathr.offline.source.SourceFormatType
import com.linkedin.feathr.offline.util.SourceUtils.getPathList
import org.apache.hadoop.conf.Configuration
import org.testng.annotations.Test

/**
 *
 */
class TestTimeBasedJoin extends TestFeathr {
  val timeAwareDailyDirectory = "src/test/resources/localTimeAwareTestFeatureData/"

  @Test
  def testStartEndDatePath(): Unit = {
    val inputData1 = InputData("/test/path/", SourceFormatType.TIME_PATH, startDate = Some("20170908"), endDate = Some("20170910"))
    val pathList11 = getPathList(inputData1.sourceType, inputData1.inputPath, ss, inputData1.dateParam, dataLoaderHandlers=List())
    val actualPathList = List("/test/path/2017/09/08", "/test/path/2017/09/09", "/test/path/2017/09/10")
    assert(pathList11 == actualPathList)
  }

  @Test
  def testStartEndDatePathWithNoFailOnMissing(): Unit = {
    // This is an actual file path directory in our file system unlike other tests
    val inputData1 = InputData(timeAwareDailyDirectory, SourceFormatType.TIME_PATH, startDate = Some("20180430"), endDate = Some("20180504"))
    // Data for 20180503 and 20180504 is missing, so it should not fail because we have the failOnMissing flag set to false.
    val pathList11 = getPathList(inputData1.sourceType, inputData1.inputPath, ss, inputData1.dateParam, targetDate=None, failOnMissing=false, dataLoaderHandlers=List())
    val actualPathList =
      List(timeAwareDailyDirectory + "daily/2018/04/30", timeAwareDailyDirectory + "daily/2018/05/01", timeAwareDailyDirectory + "daily/2018/05/02")
    assert(pathList11 == actualPathList)
  }

  @Test
  def testOffsetDatePath(): Unit = {
    val dateOffset = 12
    val numDays = 4
    val format = new SimpleDateFormat("yyyyMMdd")
    val tz = TimeZone.getTimeZone("America/Los_Angeles")
    val startDate = Calendar.getInstance()
    val endDate = Calendar.getInstance()

    format.setTimeZone(tz)

    startDate.add(Calendar.DATE, -dateOffset - numDays + 1)
    endDate.add(Calendar.DATE, -dateOffset)

    val startDateFromOffset = format.format(startDate.getTime())
    val endDateFromOffset = format.format(endDate.getTime())

    val inputData3 = InputData("/test/path/", SourceFormatType.TIME_PATH, numDays = Some("4d"), dateOffset = Some("12d"))
    val inputData4 = InputData("/test/path/", SourceFormatType.TIME_PATH, startDate = Some(startDateFromOffset), endDate = Some(endDateFromOffset))
    val pathList33 = getPathList(inputData3.sourceType, inputData3.inputPath, ss, inputData3.dateParam, dataLoaderHandlers=List())

    val pathList44 = getPathList(inputData4.sourceType, inputData4.inputPath, ss, inputData4.dateParam, dataLoaderHandlers=List())

    assert(pathList33 == pathList44)

  }

  // to test if the target date is added, relative dateParam will be computed based on the target date
  @Test
  def testOffsetDatePathRelatedToObs(): Unit = {
    val observationStartDate = "20180228"
    // relative date
    val dateOffset = "1d"
    val numDays = "1d"

    val featureDataRelative =
      InputData("FEATHR_TEST_ONLY/path/", SourceFormatType.TIME_PATH, numDays = Some(numDays), dateOffset = Some(dateOffset))

    val featureDataRelative2 = InputData("FEATHR_TEST_ONLY/path/", SourceFormatType.TIME_PATH, startDate = Some("20180227"), endDate = Some("20180227"))

    val pathListRelative =
    getPathList(featureDataRelative.sourceType, featureDataRelative.inputPath, ss, featureDataRelative.dateParam, targetDate=Some(observationStartDate), dataLoaderHandlers=List())
    val pathListRelative2 = getPathList(featureDataRelative2.sourceType, featureDataRelative2.inputPath, ss, featureDataRelative2.dateParam,dataLoaderHandlers=List())

    assert(pathListRelative.size == 1)
    assert(pathListRelative == pathListRelative2)

    // specific date
    val startDate = "20180225"
    val endDate = "20180225"
    val featureDataSpecific = InputData("FEATHR_TEST_ONLY/path/", SourceFormatType.TIME_PATH, startDate = Some(startDate), endDate = Some(endDate))
    val pathListSpecific =
    getPathList(featureDataSpecific.sourceType, featureDataSpecific.inputPath, ss, featureDataSpecific.dateParam, targetDate=Some(observationStartDate), dataLoaderHandlers=List())
    val pathListSpecific2 = getPathList(featureDataSpecific.sourceType, featureDataSpecific.inputPath, ss, featureDataSpecific.dateParam, dataLoaderHandlers=List())

    assert(pathListSpecific == pathListSpecific2)

  }

  @Test
  def testFixedPathList(): Unit = {
    val conf = new Configuration()
    val inputData2 = InputData("/test/path/", SourceFormatType.FIXED_PATH)
    val pathList22 = getPathList(inputData2.sourceType, inputData2.inputPath, ss, inputData2.dateParam,dataLoaderHandlers=List())
    assert(pathList22 == List("/test/path"))
  }

  @Test(expectedExceptions = Array(classOf[RuntimeException]))
  def testWrongTimeParam1(): Unit = {
    val inputData5 = InputData("/test/path/", SourceFormatType.TIME_PATH, startDate = Some("20170908"), numDays = Some("10"))
    val conf = new Configuration()
    // this should throw an exception
    getPathList(inputData5.sourceType, inputData5.inputPath, ss, inputData5.dateParam,dataLoaderHandlers=List())
  }

  @Test(expectedExceptions = Array(classOf[RuntimeException]))
  def testWrongTimeParam2(): Unit = {
    val inputData6 = InputData("/test/path/", SourceFormatType.TIME_PATH, endDate = Some("20170908"), dateOffset = Some("10"))
    val conf = new Configuration()
    // this should throw an exception
    getPathList(inputData6.sourceType, inputData6.inputPath, ss, inputData6.dateParam,dataLoaderHandlers=List())
  }

  @Test(expectedExceptions = Array(classOf[RuntimeException]))
  def testWrongTimeParam3(): Unit = {
    val inputData7 = InputData("/test/path/", SourceFormatType.TIME_PATH, numDays = Some("7"), dateOffset = Some("-10"))
    val conf = new Configuration()
    // this should throw an exception
    getPathList(inputData7.sourceType, inputData7.inputPath, ss, inputData7.dateParam,dataLoaderHandlers=List())
  }

  @Test(expectedExceptions = Array(classOf[RuntimeException]))
  def testWrongTimeParam4(): Unit = {
    val inputData8 = InputData("/test/path/", SourceFormatType.TIME_PATH, numDays = Some("0"), dateOffset = Some("10"))
    val conf = new Configuration()
    // this should throw an exception
    getPathList(inputData8.sourceType, inputData8.inputPath, ss, inputData8.dateParam,dataLoaderHandlers=List())
  }
}