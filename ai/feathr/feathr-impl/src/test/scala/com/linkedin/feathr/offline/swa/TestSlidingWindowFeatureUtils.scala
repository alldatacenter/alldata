package com.linkedin.feathr.offline.swa

import java.time.{DateTimeException, LocalDate}
import com.linkedin.feathr.common.DateParam
import com.linkedin.feathr.common.exception.FeathrConfigException
import com.linkedin.feathr.offline.TestFeathr
import com.linkedin.feathr.offline.anchored.feature.{FeatureAnchor, FeatureAnchorWithSource}
import com.linkedin.feathr.offline.config.{JoinTimeSetting, ObservationDataTimeSetting, TimestampColumn}
import com.linkedin.feathr.offline.job.PreprocessedDataFrameManager
import com.linkedin.feathr.offline.source.{DataSource, SourceFormatType, TimeWindowParams}
import com.linkedin.feathr.sparkcommon.SourceKeyExtractor
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row}
import org.testng.Assert.{assertEquals, assertFalse, assertTrue}
import org.testng.annotations._
import org.scalatest.mockito.MockitoSugar.mock
import org.mockito.Mockito.when

import scala.collection.JavaConverters._

class TestSlidingWindowFeatureUtils extends TestFeathr {

  var mockDataFrame: DataFrame = _

  private def createDataFrameWithMockData(): DataFrame = {
    // Create a dataframe where obsCol1 and obsCol2 are part of the observation data, and f1NumericType, f2StringType belong to the feature data.
    /*
     * x  timestamp
     * 1         2020/08/03
     * 2         2020/08/05
     * 3         2020/08/02
     */
    val dfSchema = StructType(List(StructField("x", StringType), StructField("timestamp", StringType)))

    // Values for the dataframe
    val values = List(Row("1", "2020/08/03"), Row("2", "2020/08/05"), Row("3", "2020/08/02"))

    ss.createDataFrame(values.asJava, dfSchema)
  }

  @BeforeClass
  def prepare(): Unit = {
    mockDataFrame = createDataFrameWithMockData()
  }

  /**
   * Test constructing timestamp with daylight saving at:
   * Sunnyvale (USA - California)	Friday, May 3, 2019 0:00:00 GMT-07:00 DST
   * Corresponding UTC (GMT)	Friday, May 3, 2019 7:00:00
   * Epoch: 1556866800
   */
  @Test(enabled = true)
  def testTimestampWithFormatDayMonthYearFormat(): Unit = {
    val timeStampFormat = "dd-MM-yyyy"
    val expr = SlidingWindowFeatureUtils.constructTimeStampExpr("\"03-05-2019\"", timeStampFormat)

    val epoch = ss.sql(s"""select $expr
       """.stripMargin)

    assertEquals(epoch.first().getLong(0), 1556866800)
  }

  /**
   * Test constructing timestamp with daylight saving at:
   * Sunnyvale (USA - California)	Friday, May 3, 2019 0:00:00 GMT-07:00 DST
   * Corresponding UTC (GMT)	Friday, May 3, 2019 7:00:00
   * Epoch: 1556866800
   */
  @Test(enabled = true)
  def testTimestampWithMonthDayYearFormat(): Unit = {
    val timeStampFormat = "MM-dd-yyyy"
    val expr = SlidingWindowFeatureUtils.constructTimeStampExpr("\"05-03-2019\"", timeStampFormat)

    val epoch = ss.sql(s"""select $expr
       """.stripMargin)

    assertEquals(epoch.first().getLong(0), 1556866800)
  }

  @Test(enabled = true)
  def testTimestampWithMilliSecondsTime(): Unit = {
    val timeStampFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    val expr = SlidingWindowFeatureUtils.constructTimeStampExpr("\"2019-05-03T15:00:00.123Z\"", timeStampFormat, Some("America/Los_Angeles"))

    val epoch = ss.sql(s"""select $expr
       """.stripMargin)

    assertEquals(epoch.first().getLong(0), 1556920800)
  }

  /**
   * Test constructing timestamp with daylight saving at:
   * Sunnyvale (USA - California)	Friday, May 3, 2019 0:00:00 GMT-07:00 DST
   * Corresponding UTC (GMT)	Friday, May 3, 2019 7:00:00
   * Epoch: 1556866800
   */
  @Test(enabled = true)
  def testTimestampWithYearMonthDayFormat(): Unit = {
    val timeStampFormat = "yyyy-MM-dd"
    val expr = SlidingWindowFeatureUtils.constructTimeStampExpr("\"2019-05-03\"", timeStampFormat)

    val epoch = ss.sql(s"""select $expr
       """.stripMargin)

    assertEquals(epoch.first().getLong(0), 1556866800)
  }

  /**
   * Test constructing timestamp with daylight saving at:
   * Sunnyvale (USA - California)	Friday, May 3, 2019 at 3:00:00 pm
   * Corresponding UTC (GMT)	Friday, May 3, 2019 at 22:00:00
   * Epoch: 1556920800
   */
  @Test(enabled = true)
  def testTimestampWithDefaultTimezone(): Unit = {
    val timeStampFormat = "yyyy-MM-dd HH:mm:ss"
    val expr = SlidingWindowFeatureUtils.constructTimeStampExpr("\"2019-05-03 15:00:00\"", timeStampFormat)

    val epoch = ss.sql(s"""select $expr
       """.stripMargin)

    assertEquals(epoch.first().getLong(0), 1556920800)
  }

  /**
   * Test constructing timestamp with daylight saving at:
   * Input timezone: UTC
   * Daylight saving
   * UTC: Friday, May 3, 2019 15:00:00
   * Epoc: 1556895600
   */
  @Test(enabled = true)
  def testTimestampWithUtc(): Unit = {
    val timeStampFormat = "yyyy-MM-dd HH:mm:ss"
    val expr = SlidingWindowFeatureUtils.constructTimeStampExpr("\"2019-05-03 15:00:00\"", timeStampFormat, Some("Z"))

    val epoch = ss.sql(s"""select $expr
       """.stripMargin)

    assertEquals(epoch.first().getLong(0), 1556895600)
  }

  /**
   * Test constructing timestamp with wrong timezone name.
   */
  @Test(enabled = true, expectedExceptions = Array(classOf[DateTimeException]))
  def testTimestampWithWrongTimezone(): Unit = {
    val timeStampFormat = "yyyy-MM-dd HH:mm:ss"
    val expr = SlidingWindowFeatureUtils.constructTimeStampExpr("\"2019-05-03 15:00:00\"", timeStampFormat, Some("Asia/Los Angele"))

    val epoch = ss.sql(s"""select $expr
       """.stripMargin)

    assertEquals(epoch.first().getLong(0), 1556863200)
  }

  /**
   * Test constructing timestamp with daylight saving at:
   * Input timezone: Asia/Seoul
   * Daylight saving
   * Asia/Seoul: Friday, May 3, 2019 at 3:00:00 pm
   * Thursday, May 2, 2019 at 11:00:00 pm
   * UTC: Friday, May 3, 2019 at 06:00:00
   * Epoc: 1556863200
   */
  @Test(enabled = true)
  def testTimestampWithPDTSeoul(): Unit = {
    val timeStampFormat = "yyyy-MM-dd HH:mm:ss"
    val expr = SlidingWindowFeatureUtils.constructTimeStampExpr("\"2019-05-03 15:00:00\"", timeStampFormat, Some("Asia/Seoul"))

    val epoch = ss.sql(s"""select $expr
       """.stripMargin)

    assertEquals(epoch.first().getLong(0), 1556863200)
  }

  /**
   * Test constructing timestamp with daylight saving at:
   * Input timezone: USA - New York
   * Daylight saving
   * Albany (USA - New York)	Friday, May 3, 2019 at 3:00:00 pm
   * Sunnyvale (USA - California)	Friday, May 3, 2019 at 12:00:00 noon
   * Corresponding UTC (GMT)	Friday, May 3, 2019 at 19:00:00
   * Epoc:
   */
  @Test(enabled = true)
  def testTimestampWithPDTNewYork(): Unit = {
    val timeStampFormat = "yyyy-MM-dd HH:mm:ss"
    val expr = SlidingWindowFeatureUtils.constructTimeStampExpr("\"2019-05-03 15:00:00\"", timeStampFormat, Some("America/New_York"))

    val epoch = ss.sql(s"""select $expr
       """.stripMargin)

    assertEquals(epoch.first().getLong(0), 1556910000)
  }

  /**
   * Test constructing timestamp with daylight saving at:
   * Daylight saving
   * Sunnyvale (USA - California)	Friday, May 3, 2019 at 3:00:00 pm
   * Corresponding UTC (GMT)	Friday, May 3, 2019 at 22:00:00
   * Epoc: 1556920800
   */
  @Test(enabled = true)
  def testTimestampWithPDTLos_Angeles(): Unit = {
    val timeStampFormat = "yyyy-MM-dd HH:mm:ss"
    val expr = SlidingWindowFeatureUtils.constructTimeStampExpr("\"2019-05-03 15:00:00\"", timeStampFormat, Some("America/Los_Angeles"))

    val epoch = ss.sql(s"""select $expr
       """.stripMargin)

    assertEquals(epoch.first().getLong(0), 1556920800)
  }

  /**
   * Test constructing timestamp with daylight saving at:
   * Standard time(not daylight saving)
   * Sunnyvale: Thursday, January 3, 2019 22:37:00 GMT-08:00
   * Corresponding UTC (GMT): Friday, January 4, 2019 6:37:00
   * Epoch: 1546583820
   */
  @Test(enabled = true)
  def testTimestampWithPST(): Unit = {
    val timeStampFormat = "yyyy-MM-dd HH:mm:ss"
    val expr = SlidingWindowFeatureUtils.constructTimeStampExpr("\"2019-01-03 22:37:00\"", timeStampFormat)

    val epoch = ss.sql(s"""select $expr
       """.stripMargin)

    assertEquals(epoch.first().getLong(0), 1546583820)
  }

  /**
   * Test constructing timestamp with epoch format:
   * Standard time(not daylight saving)
   * Epoch: 1546583820
   */
  @Test(enabled = true)
  def testTimestampWithEpoch(): Unit = {
    val timeStampFormat = "epoch"
    val expr = SlidingWindowFeatureUtils.constructTimeStampExpr("\"1546583820\"", timeStampFormat)

    val epoch = ss.sql(s"""select $expr
       """.stripMargin)

    assertEquals(epoch.first().getLong(0), 1546583820)
  }

  /**
   * Test getting time interval for a df mockdataframe with timestamp column
   */
  @Test(enabled = true)
  def testGetObsDataTimeRangeWithTimestamp(): Unit = {
    val joinTimeSetting = JoinTimeSetting(TimestampColumn("timestamp", "yyyy/MM/dd"), None, false)
    val dateTimeInterval = SlidingWindowFeatureUtils.getObsDataTimeRange(mockDataFrame, None, Some(joinTimeSetting))

    assertEquals(dateTimeInterval.getStart.toLocalDate, LocalDate.of(2020, 8, 2))
    assertEquals(dateTimeInterval.getEnd.toLocalDate, LocalDate.of(2020, 8, 5))
  }

  /**
   * Test getting time interval for a df mockdataframe with absolute time range
   */
  @Test(enabled = true)
  def testGetObsDataTimeRangeWithUseLatestFeatureData(): Unit = {
    val observationDataTimeSettings = ObservationDataTimeSetting(DateParam(Some("2020/08/01"), Some("2020/08/03")), Some("yyyy/MM/dd"))
    val dateTimeInterval = SlidingWindowFeatureUtils.getObsDataTimeRange(mockDataFrame, Some(observationDataTimeSettings), None)

    assertEquals(dateTimeInterval.getStart.toLocalDate, LocalDate.of(2020, 8, 1))
    // It will be both the days inclusive, so the taking the upper bound till 2020-08-04T07:00:00Z
    assertEquals(dateTimeInterval.getEnd.toLocalDate, LocalDate.of(2020, 8, 4))
  }

  /**
   * If the timeWindowParams is defined in the source, just return it.
   */
  @Test
  def testGetTimeWindowParamsWithExistingColumn(): Unit = {
    val timeWindowParams = TimeWindowParams("col", "yyyy/MM/dd")
    val source = DataSource("testPath", SourceFormatType.FIXED_PATH, Some(timeWindowParams))
    assertEquals(SlidingWindowFeatureUtils.getTimeWindowParam(source), timeWindowParams)
  }

  /**
   * If the timeWindowParams is not defined in the source, return the column that will be created from the time partition
   */
  @Test
  def testGetTimeWindowParamsFromTimePartition(): Unit = {
    val source = DataSource("testPath/daily/", SourceFormatType.TIME_SERIES_PATH, None, Some("yyyy/MM/dd"))
    assertEquals(SlidingWindowFeatureUtils.getTimeWindowParam(source), TimeWindowParams("__feathr_timestamp_column_from_partition", "epoch"))
  }

  /**
   * don't need to create the column if timeWindowParams is set in the source.
   */
  @Test
  def testNeedCreateTimestampColumnFromPartitionWithTimeWindowParamsDefined(): Unit = {
    val source = DataSource("testPath", SourceFormatType.FIXED_PATH, Some(TimeWindowParams("col", "yyyy/MM/dd")))
    assertFalse(SlidingWindowFeatureUtils.needCreateTimestampColumnFromPartition(source))
  }

  /**
   * need to create the column if timeWindowParams is not defined.
   */
  @Test
  def testNeedCreateTimestampColumnFromPartitionWithTimePartitionDefined(): Unit = {
    val source = DataSource("testPath/daily/", SourceFormatType.TIME_SERIES_PATH, None, Some("yyyy/MM/dd"))
    assertTrue(SlidingWindowFeatureUtils.needCreateTimestampColumnFromPartition(source))
  }

  /**
   * throw exception if neither timeWindowParams or timePartitionPattern is defined.
   */
  @Test(
    expectedExceptions = Array(classOf[FeathrConfigException]),
    expectedExceptionsMessageRegExp =
      ".*The source testPath is used in sliding window aggregation, but neither timeWindowParams or timePartitionPattern is defined.")
  def testNeedCreateTimestampColumnFromPartitionWithoutTimeWindowParamOrTimePartition(): Unit = {
    val source = DataSource("testPath", SourceFormatType.FIXED_PATH, None, None)
    SlidingWindowFeatureUtils.needCreateTimestampColumnFromPartition(source)
  }


  @Test(description = "getSWAAnchorGroups should group FeatureAnchorWithSource with same different source," +
    "key extractor and lateral view param into the same group")
  def testGetSWAAnchorGroupsWithSameAnchor(): Unit = {
    val mockSource1 = mock[DataSource]
    val mockSourceKeyExtractor1 = mock[SourceKeyExtractor]
    when(mockSourceKeyExtractor1.toString).thenReturn("keyExtractor1")
    val mockFeatureAnchor1 = mock[FeatureAnchor]
    when(mockFeatureAnchor1.sourceKeyExtractor).thenReturn(mockSourceKeyExtractor1)

    val baseAnchorWithSource = mock[FeatureAnchorWithSource]
    when(baseAnchorWithSource.featureAnchor).thenReturn(mockFeatureAnchor1)
    when(baseAnchorWithSource.source).thenReturn(mockSource1)

    val windowAggAnchorDFThisStage: Map[FeatureAnchorWithSource, DataFrame] =
      Seq(baseAnchorWithSource, baseAnchorWithSource).map(source =>
        (source, mockDataFrame)
      ).toMap
    val groups = SlidingWindowFeatureUtils.getSWAAnchorGroups(windowAggAnchorDFThisStage)
    assertTrue(groups.size == 1)
  }

  @Test(description = "getSWAAnchorGroups should group FeatureAnchorWithSource with different " +
    "key extractors into different groups")
  def testGetSWAAnchorGroupsWithDiffKeyExtractor(): Unit = {
    val mockSource1 = mock[DataSource]
    val mockSourceKeyExtractor1 = mock[SourceKeyExtractor]
    when(mockSourceKeyExtractor1.toString).thenReturn("keyExtractor1")
    val mockFeatureAnchor1 = mock[FeatureAnchor]
    when(mockFeatureAnchor1.sourceKeyExtractor).thenReturn(mockSourceKeyExtractor1)


    val mockSourceKeyExtractor2 = mock[SourceKeyExtractor]
    when(mockSourceKeyExtractor2.toString).thenReturn("keyExtractor2")
    val mockFeatureAnchor2 = mock[FeatureAnchor]
    when(mockFeatureAnchor2.sourceKeyExtractor).thenReturn(mockSourceKeyExtractor2)

    val baseAnchorWithSource = mock[FeatureAnchorWithSource]
    when(baseAnchorWithSource.featureAnchor).thenReturn(mockFeatureAnchor1)
    when(baseAnchorWithSource.source).thenReturn(mockSource1)

    val baseAnchorWithSourceDiffKey = mock[FeatureAnchorWithSource]
    when(baseAnchorWithSourceDiffKey.featureAnchor).thenReturn(mockFeatureAnchor2)
    when(baseAnchorWithSourceDiffKey.source).thenReturn(mockSource1)

    val windowAggAnchorDFThisStage: Map[FeatureAnchorWithSource, DataFrame] =
      Seq(baseAnchorWithSource, baseAnchorWithSourceDiffKey).map(source =>
        (source, mockDataFrame)
      ).toMap
    val groups = SlidingWindowFeatureUtils.getSWAAnchorGroups(windowAggAnchorDFThisStage)
    assertTrue(groups.size == 2)
  }

  @Test(description = "getSWAAnchorGroups should group FeatureAnchorWithSource with different " +
    "lateral view params into different groups")
  def testGetSWAAnchorGroupsWithDiffLateralView(): Unit = {
    val mockSource1 = mock[DataSource]
    val mockSourceKeyExtractor1 = mock[SourceKeyExtractor]
    when(mockSourceKeyExtractor1.toString).thenReturn("keyExtractor1")
    val mockFeatureAnchor1 = mock[FeatureAnchor]
    when(mockFeatureAnchor1.sourceKeyExtractor).thenReturn(mockSourceKeyExtractor1)
    val mockLateralViewParam2 = None

    val mockFeatureAnchor2 = mock[FeatureAnchor]
    when(mockFeatureAnchor2.lateralViewParams).thenReturn(mockLateralViewParam2)
    when(mockFeatureAnchor2.sourceKeyExtractor).thenReturn(mockSourceKeyExtractor1)

    val baseAnchorWithSource = mock[FeatureAnchorWithSource]
    when(baseAnchorWithSource.featureAnchor).thenReturn(mockFeatureAnchor1)
    when(baseAnchorWithSource.source).thenReturn(mockSource1)

    val baseAnchorWithSourceDiffView = mock[FeatureAnchorWithSource]
    when(baseAnchorWithSourceDiffView.featureAnchor).thenReturn(mockFeatureAnchor2)
    when(baseAnchorWithSourceDiffView.source).thenReturn(mockSource1)

    val windowAggAnchorDFThisStage: Map[FeatureAnchorWithSource, DataFrame] =
      Seq(baseAnchorWithSource, baseAnchorWithSourceDiffView).map(source =>
        (source, mockDataFrame)
      ).toMap
    val groups = SlidingWindowFeatureUtils.getSWAAnchorGroups(windowAggAnchorDFThisStage)
    assertTrue(groups.size == 2)
  }

  @Test(description = "getSWAAnchorGroups should group FeatureAnchorWithSource with different " +
    "sources into different groups")
  def testGetSWAAnchorGroupsWithDiffSource(): Unit = {
    val mockSource1 = mock[DataSource]
    val mockSource2 = mock[DataSource]
    val mockSourceKeyExtractor1 = mock[SourceKeyExtractor]
    when(mockSourceKeyExtractor1.toString).thenReturn("keyExtractor1")
    val mockFeatureAnchor1 = mock[FeatureAnchor]
    when(mockFeatureAnchor1.sourceKeyExtractor).thenReturn(mockSourceKeyExtractor1)

    val baseAnchorWithSource = mock[FeatureAnchorWithSource]
    when(baseAnchorWithSource.featureAnchor).thenReturn(mockFeatureAnchor1)
    when(baseAnchorWithSource.source).thenReturn(mockSource1)

    val baseAnchorWithSourceDiffSource = mock[FeatureAnchorWithSource]
    when(baseAnchorWithSourceDiffSource.featureAnchor).thenReturn(mockFeatureAnchor1)
    when(baseAnchorWithSourceDiffSource.source).thenReturn(mockSource2)

    val windowAggAnchorDFThisStage: Map[FeatureAnchorWithSource, DataFrame] =
      Seq(baseAnchorWithSource, baseAnchorWithSourceDiffSource).map(source =>
        (source, mockDataFrame)
      ).toMap
    val groups = SlidingWindowFeatureUtils.getSWAAnchorGroups(windowAggAnchorDFThisStage)
    assertTrue(groups.size == 2)
  }

  @Test(description = "When there is preprocessing for the anchor, it should make the anchors unique.")
  def testGetSWAAnchorGroupsWithSameAnchorDiffPreprocessing(): Unit = {
    PreprocessedDataFrameManager.preprocessedDfMap = Map("f1,f2" -> mock[DataFrame], "f3,f4" -> mock[DataFrame])

    val mockSource1 = mock[DataSource]
    val mockSourceKeyExtractor1 = mock[SourceKeyExtractor]
    when(mockSourceKeyExtractor1.toString).thenReturn("keyExtractor1")
    val mockFeatureAnchor1 = mock[FeatureAnchor]
    when(mockFeatureAnchor1.sourceKeyExtractor).thenReturn(mockSourceKeyExtractor1)
    when(mockFeatureAnchor1.features).thenReturn(Set("f1,f2"))


    val mockSourceKeyExtractor2 = mock[SourceKeyExtractor]
    when(mockSourceKeyExtractor2.toString).thenReturn("keyExtractor1")
    val mockFeatureAnchor2 = mock[FeatureAnchor]
    when(mockFeatureAnchor2.sourceKeyExtractor).thenReturn(mockSourceKeyExtractor2)
    when(mockFeatureAnchor2.features).thenReturn(Set("f3,f4"))

    val baseAnchorWithSource = mock[FeatureAnchorWithSource]
    when(baseAnchorWithSource.featureAnchor).thenReturn(mockFeatureAnchor1)
    when(baseAnchorWithSource.source).thenReturn(mockSource1)

    val baseAnchorWithSourceDiffKey = mock[FeatureAnchorWithSource]
    when(baseAnchorWithSourceDiffKey.featureAnchor).thenReturn(mockFeatureAnchor2)
    when(baseAnchorWithSourceDiffKey.source).thenReturn(mockSource1)

    val windowAggAnchorDFThisStage: Map[FeatureAnchorWithSource, DataFrame] =
      Seq(baseAnchorWithSource, baseAnchorWithSourceDiffKey).map(source =>
        (source, mockDataFrame)
      ).toMap
    val groups = SlidingWindowFeatureUtils.getSWAAnchorGroups(windowAggAnchorDFThisStage)
    assertTrue(groups.size == 2)
    PreprocessedDataFrameManager.preprocessedDfMap = Map()
  }
}
