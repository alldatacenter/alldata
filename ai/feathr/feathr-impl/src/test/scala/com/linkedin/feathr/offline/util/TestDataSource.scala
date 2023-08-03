package com.linkedin.feathr.offline.util

import java.time._
import com.linkedin.feathr.offline.TestFeathr
import com.linkedin.feathr.offline.TestUtils.createIntervalFromLocalTime
import com.linkedin.feathr.offline.config.location.SimplePath
import com.linkedin.feathr.offline.source.accessor.DataSourceAccessor
import com.linkedin.feathr.offline.source.{DataSource, SourceFormatType}
import com.linkedin.feathr.offline.util.datetime.OfflineDateTimeUtils
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row}
import org.testng.Assert._
import org.testng.annotations.{AfterClass, Test}

import scala.collection.JavaConverters._

class TestDataSource extends TestFeathr {
  private val simulateTimeDelays = Array[Duration]()
  override def setupSpark(): Unit = {
    ss = TestFeathr.getOrCreateSparkSessionWithHive
    conf = TestFeathr.getOrCreateConfWithHive
  }

  @AfterClass
  override def cleanup(): Unit = {
    super.cleanup()
  }

  /**
   * Test loading an hourly-partitioned feature dataset.
   */
  @Test
  def testGetFactDataAsDataframeDaily(): Unit = {
    val mockDataPath = "generation/daily"
    val mockSource = DataSource(mockDataPath, SourceFormatType.TIME_SERIES_PATH)

    val date2018011001 = LocalDateTime.of(2019, 5, 19, 0, 0)
    val date2018011002 = LocalDateTime.of(2019, 5, 20, 0, 0)
    val date2018011003 = LocalDateTime.of(2019, 5, 19, 0, 0)
    val date2018011004 = LocalDateTime.of(2019, 5, 22, 0, 0)

    // test: get 1 day of data
    // obs. data range: (2019-05-19, 2019-05-20)
    // window length: 1 hour
    val timeRange1 = OfflineDateTimeUtils.getFactDataTimeRange(
      createIntervalFromLocalTime(date2018011001.plusMinutes(30L), date2018011002.plusMinutes(59L)),
      Duration.ofDays(1),
      simulateTimeDelays)
    val anchorDF1 = DataSourceAccessor(ss=ss, source=mockSource, dateIntervalOpt=Some(timeRange1), expectDatumType=None, failOnMissingPartition = false, dataPathHandlers=List()).get()
    assertEquals(anchorDF1.count(), 8)

    // test: get 1 day of data
    // obs. data range: (2019-05-19, 2019-05-22)
    // window length: 1 hour
    val timeRange2 = OfflineDateTimeUtils.getFactDataTimeRange(
      createIntervalFromLocalTime(date2018011003.plusMinutes(30L), date2018011004.plusMinutes(59L)),
      Duration.ofDays(3),
      simulateTimeDelays)
    val anchorDF2 = DataSourceAccessor(ss=ss, source=mockSource, dateIntervalOpt=Some(timeRange2), expectDatumType=None, failOnMissingPartition = false, dataPathHandlers=List()).get()
    assertEquals(anchorDF2.count(), 13)
  }

  @Test(description = "test conversion of dataframe to schema")
  def testDfToSchemaConversion: Unit = {
    // Create a simple dataframe with only 2 columns
    val df: DataFrame = {
      /*
       * obsCol1  f1NumericType
       * row1        2.0
       */
      val dfSchema = StructType(List(StructField("obsCol1", StringType), StructField("f1NumericType", DoubleType)))

      // Values for the dataframe
      val values = List(Row("row1", 2.0))

      ss.createDataFrame(values.asJava, dfSchema)
    }

    val schema = SourceUtils.getSchemaOfDF(df)

    assertEquals(schema.getFields.size(), 2)
    assertEquals(schema.getFields.get(0).name(), "obsCol1")
    assertEquals(schema.getFields.get(1).name(), "f1NumericType")
  }

  @Test(description = "Test resolve latest")
  def testResolveLatest(): Unit = {
      val path = SimplePath("src/test/resources/decayTest/daily/#LATEST/#LATEST/#LATEST")
      assertEquals(new DataSource(path, SourceFormatType.FIXED_PATH, None, None, None).path,
        "src/test/resources/decayTest/daily/2019/05/20")
  }
}
