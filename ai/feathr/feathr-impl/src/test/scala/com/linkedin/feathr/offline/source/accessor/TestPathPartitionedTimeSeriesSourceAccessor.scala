package com.linkedin.feathr.offline.source.accessor

import com.linkedin.feathr.common.DateTimeResolution
import com.linkedin.feathr.common.exception.FeathrInputDataException
import com.linkedin.feathr.offline.TestFeathr
import com.linkedin.feathr.offline.TestUtils.{createDailyInterval, createHourlyInterval}
import com.linkedin.feathr.offline.source.{DataSource, SourceFormatType}
import com.linkedin.feathr.offline.util.PartitionLimiter
import org.apache.spark.sql.DataFrame
import org.testng.Assert.{assertEquals, assertFalse, assertTrue}
import org.testng.annotations.{BeforeClass, Test}

class TestPathPartitionedTimeSeriesSourceAccessor extends TestFeathr {
  private var dailySourceAccessorWithFailOnMissingPartition: PathPartitionedTimeSeriesSourceAccessor = _
  private var hourlySourceAccessorWithFailOnMissingPartition: PathPartitionedTimeSeriesSourceAccessor = _

  // Source accessors with failOnMissingPartitions set to false
  private var dailySourceAccessorWithoutFailOnMissingPartition: PathPartitionedTimeSeriesSourceAccessor = _
  private var hourlySourceAccessorWithoutFailOnMissingPartition: PathPartitionedTimeSeriesSourceAccessor = _

  @BeforeClass
  override def setup(): Unit = {
    super.setup()
    val sqlContext = ss.sqlContext
    import sqlContext.implicits._
    dailySourceAccessorWithFailOnMissingPartition = new PathPartitionedTimeSeriesSourceAccessor(
      ss,
      Seq(
        DatePartition(Seq("2019-05-19").toDF(), createDailyInterval("2019-05-19", "2019-05-20")),
        DatePartition(Seq("2019-05-20").toDF(), createDailyInterval("2019-05-20", "2019-05-21"))),
      DataSource("fakeDailySource", SourceFormatType.TIME_SERIES_PATH),
      createDailyInterval("2019-05-19", "2019-05-21"),
      DateTimeResolution.DAILY,
      failOnMissingPartition = true,
      false,
      new PartitionLimiter(ss))

    hourlySourceAccessorWithFailOnMissingPartition = new PathPartitionedTimeSeriesSourceAccessor(
      ss,
      Seq(
        DatePartition(Seq("2019-05-19-01").toDF(), createHourlyInterval("2019-05-19-01", "2019-05-19-02")),
        DatePartition(Seq("2019-05-19-02").toDF(), createHourlyInterval("2019-05-19-02", "2019-05-19-03"))),
      DataSource("fakeHourlySource", SourceFormatType.TIME_SERIES_PATH),
      createDailyInterval("2019-05-19-01", "2019-05-19-03"),
      DateTimeResolution.HOURLY,
      failOnMissingPartition = true,
      false,
      new PartitionLimiter(ss))

    // Build similar source accessors but with failOnMissingPartitions set to false.
    // These source accessors will ignore the missing partitions.
    dailySourceAccessorWithoutFailOnMissingPartition = new PathPartitionedTimeSeriesSourceAccessor(
      ss,
      Seq(
        DatePartition(Seq("2019-05-19").toDF(), createDailyInterval("2019-05-19", "2019-05-20")),
        DatePartition(Seq("2019-05-20").toDF(), createDailyInterval("2019-05-20", "2019-05-21"))),
      DataSource("fakeDailySource", SourceFormatType.TIME_SERIES_PATH),
      createDailyInterval("2019-05-19", "2019-05-21"),
      DateTimeResolution.DAILY,
      failOnMissingPartition = false, // Set failOnMissingPartitions to false
      false,
      new PartitionLimiter(ss))

    hourlySourceAccessorWithoutFailOnMissingPartition = new PathPartitionedTimeSeriesSourceAccessor(
      ss,
      Seq(
        DatePartition(Seq("2019-05-19-01").toDF(), createHourlyInterval("2019-05-19-01", "2019-05-19-02")),
        DatePartition(Seq("2019-05-19-02").toDF(), createHourlyInterval("2019-05-19-02", "2019-05-19-03"))),
      DataSource("fakeHourlySource", SourceFormatType.TIME_SERIES_PATH),
      createDailyInterval("2019-05-19-01", "2019-05-19-03"),
      DateTimeResolution.HOURLY,
      failOnMissingPartition = false, // Set failOnMissingPartitions to false
      false,
      new PartitionLimiter(ss))
  }

  @Test(description = "test get all partitions")
  def testGetAllPartitions(): Unit = {
    assertEquals(getTimestamps(dailySourceAccessorWithFailOnMissingPartition.get()), List("2019-05-19", "2019-05-20"))
  }

  @Test(description = "test get a subset with an interval")
  def testGetWithInterval(): Unit = {
    assertEquals(getTimestamps(dailySourceAccessorWithoutFailOnMissingPartition.get(Some(createDailyInterval("2019-05-19", "2019-05-20")))), List("2019-05-19"))
    assertEquals(getTimestamps(dailySourceAccessorWithoutFailOnMissingPartition.get(Some(createDailyInterval("2019-05-19", "2019-05-20")))), List("2019-05-19"))
  }

  @Test(description = "test overlapWithInterval")
  def testOverlapWithInterval(): Unit = {
    assertTrue(dailySourceAccessorWithFailOnMissingPartition.overlapWithInterval(createDailyInterval("2019-05-19", "2019-05-20")))
    assertFalse(dailySourceAccessorWithFailOnMissingPartition.overlapWithInterval(createDailyInterval("2019-05-21", "2019-05-22")))
  }

  @Test(description = "test get with partially missing interval does not fail with FailOnMissingPartition false")
  def testGetWithPartiallyMissingInterval(): Unit = {
    val missingInterval = Some(createDailyInterval("2019-05-20", "2019-05-22"))
    assertEquals(getTimestamps(dailySourceAccessorWithoutFailOnMissingPartition.get(missingInterval)), List("2019-05-20"))
  }

  @Test(description = "test get with partially missing interval fails when FailOnMissingPartition is true")
  def testGetFailsWithPartiallyMissingIntervalAndFailOnMissingPartitionSet(): Unit = {
    val missingInterval = Some(createDailyInterval("2019-05-20", "2019-05-22"))
    assertThrows[FeathrInputDataException] { dailySourceAccessorWithFailOnMissingPartition.get(missingInterval) }
  }

  @Test(description = "test creation with an incorrect interval", expectedExceptions = Array(classOf[FeathrInputDataException]))
  def testCreateWithIncorrectInterval(): Unit = {
    val source = DataSource("localTimeAwareTestFeatureData/daily", SourceFormatType.TIME_SERIES_PATH)
    val interval = createDailyInterval("2019-05-22", "2019-05-23")
    DataSourceAccessor(ss=ss, source=source, dateIntervalOpt=Some(interval), expectDatumType=None, failOnMissingPartition = false, dataPathHandlers=List()).asInstanceOf[PathPartitionedTimeSeriesSourceAccessor]
  }

  @Test(description = "test get all partitions with hourly dataset")
  def testGetAllPartitionsHourly(): Unit = {
    assertEquals(getTimestamps(hourlySourceAccessorWithFailOnMissingPartition.get()), List("2019-05-19-01", "2019-05-19-02"))
  }

  @Test(description = "test get a subset with an interval with hourly dataset")
  def testGetWithIntervalHourly(): Unit = {
    assertEquals(
      getTimestamps(hourlySourceAccessorWithoutFailOnMissingPartition.get(Some(createHourlyInterval("2019-05-19-01", "2019-05-19-02")))),
      List("2019-05-19-01"))
    assertEquals(
      getTimestamps(hourlySourceAccessorWithoutFailOnMissingPartition.get(Some(createHourlyInterval("2019-05-19-01", "2019-05-19-02")))),
      List("2019-05-19-01"))

  }

  @Test(description = "test overlapWithInterval with hourly dataset")
  def testOverlapWithIntervalHourly(): Unit = {
    assertTrue(hourlySourceAccessorWithFailOnMissingPartition.overlapWithInterval(createHourlyInterval("2019-05-19-01", "2019-05-19-02")))
    assertFalse(hourlySourceAccessorWithFailOnMissingPartition.overlapWithInterval(createHourlyInterval("2019-05-19-03", "2019-05-19-04")))
  }

  @Test(description = "test get with partially missing interval with hourly dataset does not fail with FailOnMissingPartition false")
  def testGetWithPartiallyMissingIntervalHourly(): Unit = {
    // test missing date partitions
    val missingInterval = Some(createHourlyInterval("2019-05-19-02", "2019-05-19-04"))
    assertEquals(getTimestamps(hourlySourceAccessorWithoutFailOnMissingPartition.get(missingInterval)), List("2019-05-19-02"))
  }

  @Test(description = "test get with partially missing interval with hourly dataset fails when FailOnMissingPartition is true")
  def testGetFailsWithPartiallyMissingIntervalHourlyAndFailOnMissingPartitionSet(): Unit = {
    // test missing date partitions
    val missingInterval = Some(createHourlyInterval("2019-05-19-02", "2019-05-19-04"))
    assertThrows[FeathrInputDataException] { hourlySourceAccessorWithFailOnMissingPartition.get(missingInterval) }
  }

  @Test(description = "test create timestamp column from the date partition.")
  def testCreateTimestampColumn(): Unit = {
    val source = DataSource("localTimeAwareTestFeatureData/daily/", SourceFormatType.TIME_SERIES_PATH, None, Some("yyyy/MM/dd"))
    val sourceInterval = Some(createDailyInterval("2018-04-30", "2018-05-02"))
    val accessor = DataSourceAccessor(ss=ss, source=source, dateIntervalOpt=sourceInterval, expectDatumType=None, failOnMissingPartition = false, addTimestampColumn = true, dataPathHandlers=List())
      .asInstanceOf[PathPartitionedTimeSeriesSourceAccessor]
    val timestamps = accessor.get().select("__feathr_timestamp_column_from_partition").collect().map(_.getLong(0)).distinct.toList
    assertEquals(
      timestamps,
      List(
        1525071600, // 2018-04-30
        1525158000 // 2018-05-01
      ))
  }

  /**
   * collect the rows of the dataframe. We assume there's only one timestamp column in the dataframe.
   * @param df the dataframe
   * @return list of timestamp string
   */
  private def getTimestamps(df: DataFrame): List[String] = {
    df.collect().map(_.getString(0)).distinct.sorted.toList
  }
}
