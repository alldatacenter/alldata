package com.linkedin.feathr.offline.transformation

import com.linkedin.feathr.offline.TestUtils.{createDailyInterval, createIntervalFromLocalTime}
import com.linkedin.feathr.offline.source.SourceFormatType._
import com.linkedin.feathr.offline.source.{DataSource, SourceFormatType, TimeWindowParams}
import com.linkedin.feathr.offline.util.HdfsUtils
import com.linkedin.feathr.offline.{TestFeathr, TestUtils}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericRecord, GenericRecordBuilder}
import org.apache.spark.sql.Row
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert.assertEquals
import org.testng.annotations.{AfterClass, Test}

import java.time._
import java.time.temporal.ChronoUnit
import scala.util.Random

/**
 * Unit test class for AnchorToDataSourceMapper.
 */
class TestAnchorToDataSourceMapper extends TestFeathr with MockitoSugar {
  private val anchorToDataSourceMapper = new AnchorToDataSourceMapper(dataPathHandlers=List())
  val testSchemaString =
    """
      |{
      |    "type": "record",
      |    "name": "WindowAggRecord",
      |    "fields": [{
      |      "name": "mId",
      |      "type": "long"
      |    }, {
      |      "name": "timestamp",
      |      "type": "long"
      |    }, {
      |      "name": "value",
      |      "type": ["null","float"],
      |      "default": null
      |    }]
      |}
    """.stripMargin

  // The path prefix where the mock test data is written to.
  val mockDataPathHead = "temp/mockData/" + getClass.getName
  val simulateTimeDelays = Array[Duration]()

  override def setupSpark(): Unit = {
    ss = TestFeathr.getOrCreateSparkSessionWithHive
    conf = TestFeathr.getOrCreateConfWithHive
  }

  @AfterClass
  override def cleanup(): Unit = {
    super.cleanup()
  }

  /**
   * Unit test for getSmallestInterval for a fixed path.
   * The calculations should not consider daysSinceLastAgg.
   */
  @Test
  def testGetSmallestIntervalForFixedPath(): Unit = {
    val intervals = Seq(
      createDailyInterval("2020-09-04", "2020-09-16"),
      createDailyInterval("2020-09-06", "2020-09-14"),
      createDailyInterval("2020-09-08", "2020-09-12"),
      createDailyInterval("2020-09-10", "2020-09-17"))
    val result = anchorToDataSourceMapper.getSmallestInterval(intervals, FIXED_PATH, Some(5))
    assertEquals(result, createDailyInterval("2020-09-04", "2020-09-17"))
  }

  /**
   * Test getSmallestInterval for time series path with no daysSinceLastAgg defined.
   */
  @Test
  def testSmallestIntervalForTimeSeriesPathAndNoAggSnapshot(): Unit = {
    val intervals = Seq(
      createDailyInterval("2020-09-04", "2020-09-16"),
      createDailyInterval("2020-09-06", "2020-09-14"),
      createDailyInterval("2020-09-08", "2020-09-12"),
      createDailyInterval("2020-09-10", "2020-09-17"))
    val result = anchorToDataSourceMapper.getSmallestInterval(intervals, TIME_SERIES_PATH, None)
    assertEquals(result, createDailyInterval("2020-09-04", "2020-09-17"))
  }

  /**
   * Test time series path with daysSinceLastAgg defined. The interval calculated
   * should move the start date back by number of days specified by daysSinceLastAgg.
   */
  @Test
  def testSmallestIntervalForTimeSeriesPathAndDaysSinceLastAgg(): Unit = {
    val intervals = Seq(
      createDailyInterval("2020-09-04", "2020-09-16"),
      createDailyInterval("2020-09-06", "2020-09-14"),
      createDailyInterval("2020-09-08", "2020-09-12"),
      createDailyInterval("2020-09-10", "2020-09-17"))
    val result = anchorToDataSourceMapper.getSmallestInterval(intervals, TIME_SERIES_PATH, Some(5))
    // 2020-09-04 - 5 days = 2020-08-29
    assertEquals(result, createDailyInterval("2020-08-29", "2020-09-17"))
  }

  /**
   * Test loading an hourly-partitioned feature dataset.
   */
  @Test
  def testGetWindowAggAnchorDFHourly(): Unit = {
    val mockDataPath = mockDataPathHead + "/testGetWindowAggAnchorDFHourly"
    val hourlyMockDataPath = mockDataPath + "/hourly"
    val hourlyMockSource = DataSource(hourlyMockDataPath, SourceFormatType.TIME_SERIES_PATH, Some(TimeWindowParams("timestamp", "epoch")))

    val date2018011001 = LocalDateTime.of(2018, 1, 10, 1, 0)
    val date2018011002 = LocalDateTime.of(2018, 1, 10, 2, 0)
    val date2018011003 = LocalDateTime.of(2018, 1, 10, 3, 0)
    val date2018011004 = LocalDateTime.of(2018, 1, 10, 4, 0)

    val paths =
      HdfsUtils.getPaths(hourlyMockDataPath, LocalDateTime.of(2018, 1, 10, 1, 0), LocalDateTime.of(2018, 1, 10, 4, 0), ChronoUnit.HOURS)

    val records2018011001 = getTestRecords(2, date2018011001, date2018011002)
    val records2018011002 = getTestRecords(2, date2018011002, date2018011003)
    val records2018011003 = getTestRecords(2, date2018011003, date2018011004)

    HdfsUtils.deletePath(mockDataPath, recursive = true, conf)

    TestUtils.writeGenericRecordToFile(this.ss, records2018011001, paths(0), testSchemaString)
    TestUtils.writeGenericRecordToFile(this.ss, records2018011002, paths(1), testSchemaString)
    TestUtils.writeGenericRecordToFile(this.ss, records2018011003, paths(2), testSchemaString)

    // test: get 1 hour of data
    // obs. data range: (2018-01-10 02:30, 2018-01-10 02:59)
    // window length: 10 min
    // should get fact data (2018-01-10 02:20, 2018-01-10 02:59)
    val anchorDF = anchorToDataSourceMapper.getWindowAggAnchorDFMapForJoin(
      this.ss,
      hourlyMockSource,
      createIntervalFromLocalTime(date2018011002.plusMinutes(30L), date2018011002.plusMinutes(59L)),
      Duration.ofMinutes(10L),
      simulateTimeDelays,
      false)

    // for systematic comparison, sort both original and loaded data by (timestamp, mId, value)
    val anchorData = anchorDF.rdd.collect().toSeq.sortBy(row => (row.getLong(1), row.getLong(0), row.getFloat(2))) // sort by timestamp then by mId
    val sortedRecords =
      records2018011002.sortBy(r => (r.get("timestamp").asInstanceOf[Long], r.get("mId").asInstanceOf[Long], r.get("value").asInstanceOf[Float]))
    assertEquals(anchorData.size, 2)
    compareTestRecordWithRddRow(sortedRecords(0), anchorData(0))
    compareTestRecordWithRddRow(sortedRecords(1), anchorData(1))

    // test: get multiple hours of data
    // obs. data range: (2018-01-10 03:00, 2018-01-10 03:59)
    // window length: 30 min
    // should get fact data (2018-01-10 02:30, 2018-01-10 03:59)
    val anchorDF2 = anchorToDataSourceMapper.getWindowAggAnchorDFMapForJoin(
      this.ss,
      hourlyMockSource,
      createIntervalFromLocalTime(date2018011003, date2018011003.plusMinutes(59L)),
      Duration.ofMinutes(30L),
      simulateTimeDelays,
      false)

    val anchorData2 = anchorDF2.rdd.collect().toSeq.sortBy(row => (row.getLong(1), row.getLong(0), row.getFloat(2)))
    val sortedRecords2 = (records2018011002 ++ records2018011003).sortBy(r =>
      (r.get("timestamp").asInstanceOf[Long], r.get("mId").asInstanceOf[Long], r.get("value").asInstanceOf[Float]))
    assertEquals(anchorData2.size, 4)
    compareTestRecordWithRddRow(sortedRecords2(0), anchorData2(0))
    compareTestRecordWithRddRow(sortedRecords2(1), anchorData2(1))
    compareTestRecordWithRddRow(sortedRecords2(2), anchorData2(2))
    compareTestRecordWithRddRow(sortedRecords2(3), anchorData2(3))
    HdfsUtils.deletePath(mockDataPath, recursive = true, conf)
  }

  /**
   * Test loading a daily-partitioned feature dataset.
   */
  @Test(enabled = true)
  def testGetWindowAggAnchorDFDaily(): Unit = {
    val mockDataPath = mockDataPathHead + "/testGetWindowAggAnchorDFDaily"
    val dailyMockDataPath = mockDataPath + "/daily"
    val dailyMockSource = DataSource(dailyMockDataPath, SourceFormatType.TIME_SERIES_PATH, Some(TimeWindowParams("timestamp", "epoch")))

    val date20180110 = LocalDate.of(2018, 1, 10)
    val date20180111 = LocalDate.of(2018, 1, 11)
    val date20180112 = LocalDate.of(2018, 1, 12)

    val paths = HdfsUtils
      .getPaths(dailyMockDataPath, LocalDate.of(2018, 1, 10).atStartOfDay(), LocalDate.of(2018, 1, 13).atStartOfDay(), ChronoUnit.DAYS)

    val records20180110 = getTestRecords(2, date20180110.atStartOfDay(), date20180110.atTime(LocalTime.MAX))
    val records20180111 = getTestRecords(2, date20180111.atStartOfDay(), date20180111.atTime(LocalTime.MAX))
    val records20180112 = getTestRecords(2, date20180111.atStartOfDay(), date20180112.atTime(LocalTime.MAX))

    HdfsUtils.deletePath(mockDataPath, recursive = true, conf)

    TestUtils.writeGenericRecordToFile(this.ss, records20180110, paths(0), testSchemaString)
    TestUtils.writeGenericRecordToFile(this.ss, records20180111, paths(1), testSchemaString)
    TestUtils.writeGenericRecordToFile(this.ss, records20180112, paths(2), testSchemaString)

    // test: get 1 day of data
    // obs. data range: (2018-01-11 06:00, 2018-01-11 23:59)
    // window length: 1hr
    // should get fact data (2018-01-11)
    val anchorDF = anchorToDataSourceMapper.getWindowAggAnchorDFMapForJoin(
      this.ss,
      dailyMockSource,
      createIntervalFromLocalTime(date20180111.atTime(6, 0), date20180111.atTime(LocalTime.MAX)),
      Duration.ofHours(1L),
      simulateTimeDelays,
      failOnMissingPartition = false)

    val anchorData = anchorDF.rdd.collect().toSeq.sortBy(row => (row.getLong(1), row.getLong(0), row.getFloat(2))) // sort by timestamp then by mId
    val sortedRecords = (records20180110 ++ records20180111).sortBy(r =>
      (r.get("timestamp").asInstanceOf[Long], r.get("mId").asInstanceOf[Long], r.get("value").asInstanceOf[Float]))
    assertEquals(anchorData.size, 4)
    compareTestRecordWithRddRow(sortedRecords(0), anchorData(0))
    compareTestRecordWithRddRow(sortedRecords(1), anchorData(1))
    compareTestRecordWithRddRow(sortedRecords(2), anchorData(2))
    compareTestRecordWithRddRow(sortedRecords(3), anchorData(3))

    // test: get multiple days of data
    // obs. data range: (2018-01-11 00:00, 2018-01-11 23:59)
    // window length: 1 day
    // should get fact data (2018-01-10 00:00, 2018-01-11 23:59)
    val anchorDF2 = anchorToDataSourceMapper.getWindowAggAnchorDFMapForJoin(
      this.ss,
      dailyMockSource,
      createIntervalFromLocalTime(date20180111.atStartOfDay(), date20180111.atTime(LocalTime.MAX)),
      Duration.ofDays(1L),
      simulateTimeDelays,
      failOnMissingPartition = false)

    val anchorData2 = anchorDF2.rdd.collect().toSeq.sortBy(row => (row.getLong(1), row.getLong(0), row.getFloat(2)))
    val sortedRecords2 = (records20180110 ++ records20180111).sortBy(r =>
      (r.get("timestamp").asInstanceOf[Long], r.get("mId").asInstanceOf[Long], r.get("value").asInstanceOf[Float]))
    assertEquals(anchorData2.size, 4)
    compareTestRecordWithRddRow(sortedRecords2(0), anchorData2(0))
    compareTestRecordWithRddRow(sortedRecords2(1), anchorData2(1))
    compareTestRecordWithRddRow(sortedRecords2(2), anchorData2(2))
    compareTestRecordWithRddRow(sortedRecords2(3), anchorData2(3))
    HdfsUtils.deletePath(mockDataPath, recursive = true, conf)
  }


  /**
   * get a certain number of test records with the testSchemaString as schema,
   * and the timestamp in the range of startTime and endTime.
   */
  private def getTestRecords(num: Int, startTime: LocalDateTime, endTime: LocalDateTime): Seq[GenericRecord] = {
    val startEpoch = startTime.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli
    val endEpoch = endTime.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli
    val rand = new Random()
    (0 until num).map(_ => {
      val t = startEpoch + rand.nextInt((endEpoch - startEpoch).toInt).toLong
      new GenericRecordBuilder(Schema.parse(testSchemaString))
        .set("mId", rand.nextInt(num).toLong)
        .set("timestamp", t)
        .set("value", rand.nextFloat())
        .build()
    })
  }

  /**
   * Utility method for comparing test records with the RDD result of data loading.
   */
  private def compareTestRecordWithRddRow(record: GenericRecord, result: Row): Unit = {
    assertEquals(result.getLong(0), record.get("mId"))
    assertEquals(result.getLong(1), record.get("timestamp"))
    assertEquals(result.getFloat(2), record.get("value"))
  }
}
