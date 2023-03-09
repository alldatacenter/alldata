package com.linkedin.feathr.offline.util.datetime

import com.linkedin.feathr.common.exception.FeathrConfigException
import com.linkedin.feathr.common.{DateParam, DateTimeResolution}
import com.linkedin.feathr.offline.TestUtils.{createDailyInterval, createHourlyInterval}
import org.scalatest.testng.TestNGSuite
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Duration, LocalDate, ZoneId, ZonedDateTime}

class TestOfflineDateTimeUtils extends TestNGSuite {

  @Test(description = "test dateRange for creating datepartition filter expression")
  def testDateRange(): Unit = {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH").withZone(OfflineDateTimeUtils.DEFAULT_ZONE_ID)
    val interval = createHourlyInterval("2007-12-03-10", "2007-12-05-20")
    assertEquals(OfflineDateTimeUtils.dateRange(interval, DateTimeResolution.DAILY), "datepartition >= '2007-12-03-00' and datepartition < '2007-12-06-00'")
    assertEquals(OfflineDateTimeUtils.dateRange(interval, DateTimeResolution.HOURLY), "datepartition >= '2007-12-03-10' and datepartition < '2007-12-05-20'")
  }

  @Test(description = "test normal daily case where input obsDataStartTime and obsDataEndTime are aligned by day and no delay")
  def testDailyGetFactDataTimeRangeAlignedNoDelay(): Unit = {
    val obsTimeRange = createHourlyInterval("2019-05-08-00", "2019-05-08-00")
    val window: Duration = Duration.ofDays(1)
    val simulateTimeDelays = Array(Duration.ZERO)
    val range = OfflineDateTimeUtils.getFactDataTimeRange(obsTimeRange, window, simulateTimeDelays)
    assertEquals(range, createHourlyInterval("2019-05-07-00", "2019-05-08-00"))
  }

  @Test(description = "test normal daily case where input obsDataStartTime and obsDataEndTime are aligned by day with delay")
  def testDailyGetFactDataTimeRangeAlignedWithDelay(): Unit = {
    val obsTimeRange = createHourlyInterval("2019-05-08-00", "2019-05-08-00")
    val window: Duration = Duration.ofDays(1)
    val simulateTimeDelays = Array(Duration.ofDays(1))
    val range = OfflineDateTimeUtils.getFactDataTimeRange(obsTimeRange, window, simulateTimeDelays)
    assertEquals(range, createHourlyInterval("2019-05-06-00", "2019-05-07-00"))
  }

  @Test(description = "test case with multiple time delays")
  def testGetFactDataRangWithMultipleTimeDelays(): Unit = {
    val obsTimeRange = createHourlyInterval("2019-05-08-01", "2019-05-08-01")
    val window: Duration = Duration.ofHours(1)
    // Different time delays for different features
    val simulateTimeDelays = Array(Duration.ofHours(1), Duration.ofHours(2), Duration.ofHours(9))
    val range = OfflineDateTimeUtils.getFactDataTimeRange(obsTimeRange, window, simulateTimeDelays)
    assertEquals(range, createHourlyInterval("2019-05-07-15", "2019-05-08-00"))
  }

  @Test(description = "test hourly normal case")
  def testHourlyGetFactDataTimeRangeNormal(): Unit = {
    val obsTimeRange = createHourlyInterval("2019-05-08-01", "2019-05-08-01")
    val window: Duration = Duration.ofHours(1)
    val simulateTimeDelays = Array(Duration.ofHours(1))
    val range = OfflineDateTimeUtils.getFactDataTimeRange(obsTimeRange, window, simulateTimeDelays)
    assertEquals(range, createHourlyInterval("2019-05-07-23", "2019-05-08-00"))
  }

  @Test(description = "test createTimeFromString with default format")
  def testCreateTimeFromString(): Unit = {
    assertEquals(OfflineDateTimeUtils.createTimeFromString("20200904"), ZonedDateTime.of(2020, 9, 4, 0, 0, 0, 0, OfflineDateTimeUtils.DEFAULT_ZONE_ID))
    // test different time zone
    assertEquals(OfflineDateTimeUtils.createTimeFromString("20200904", tz = "UTC"), ZonedDateTime.of(2020, 9, 4, 0, 0, 0, 0, ZoneId.of("UTC")))
  }

  @Test(description = "test createTimeFromString with different format strings")
  def testCreateTimeFromStringWithFormat(): Unit = {
    assertEquals(
      OfflineDateTimeUtils.createTimeFromString("20200904", "yyyyMMdd"),
      ZonedDateTime.of(2020, 9, 4, 0, 0, 0, 0, OfflineDateTimeUtils.DEFAULT_ZONE_ID))
    assertEquals(
      OfflineDateTimeUtils.createTimeFromString("2020-09-04", "yyyy-MM-dd"),
      ZonedDateTime.of(2020, 9, 4, 0, 0, 0, 0, OfflineDateTimeUtils.DEFAULT_ZONE_ID))
    assertEquals(
      OfflineDateTimeUtils.createTimeFromString("2020/09/04", "yyyy/MM/dd"),
      ZonedDateTime.of(2020, 9, 4, 0, 0, 0, 0, OfflineDateTimeUtils.DEFAULT_ZONE_ID))
    assertEquals(
      OfflineDateTimeUtils.createTimeFromString("2020//09//04", "yyyy/MM/dd"),
      ZonedDateTime.of(2020, 9, 4, 0, 0, 0, 0, OfflineDateTimeUtils.DEFAULT_ZONE_ID))
    // with hour
    assertEquals(
      OfflineDateTimeUtils.createTimeFromString("2020090414", "yyyyMMddHH"),
      ZonedDateTime.of(2020, 9, 4, 14, 0, 0, 0, OfflineDateTimeUtils.DEFAULT_ZONE_ID))
    assertEquals(
      OfflineDateTimeUtils.createTimeFromString("2020-09-04-14", "yyyy-MM-dd-HH"),
      ZonedDateTime.of(2020, 9, 4, 14, 0, 0, 0, OfflineDateTimeUtils.DEFAULT_ZONE_ID))
    assertEquals(
      OfflineDateTimeUtils.createTimeFromString("2020/09/04/14", "yyyy/MM/dd/HH"),
      ZonedDateTime.of(2020, 9, 4, 14, 0, 0, 0, OfflineDateTimeUtils.DEFAULT_ZONE_ID))
    assertEquals(
      OfflineDateTimeUtils.createTimeFromString("2020//09//04//14", "yyyy/MM/dd/HH"),
      ZonedDateTime.of(2020, 9, 4, 14, 0, 0, 0, OfflineDateTimeUtils.DEFAULT_ZONE_ID))
  }

  @Test(description = "test createTimeIntervalFromDateParam with invalid date parameters")
  def createTimeIntervalFromDateParamInvalidInput(): Unit = {
    assertThrows[FeathrConfigException](OfflineDateTimeUtils.createTimeIntervalFromDateParam(None, None))
    assertThrows[FeathrConfigException](OfflineDateTimeUtils.createTimeIntervalFromDateParam(Some(DateParam(None, None, None, None)), None))
    assertThrows[FeathrConfigException](OfflineDateTimeUtils.createTimeIntervalFromDateParam(Some(DateParam(Some("20200904"), None, None, None)), None))
    assertThrows[FeathrConfigException](OfflineDateTimeUtils.createTimeIntervalFromDateParam(Some(DateParam(None, Some("20200909"), None, None)), None))
    assertThrows[FeathrConfigException](OfflineDateTimeUtils.createTimeIntervalFromDateParam(Some(DateParam(None, None, Some("1d"), None)), None))
    assertThrows[FeathrConfigException](
      OfflineDateTimeUtils.createTimeIntervalFromDateParam(Some(DateParam(Some("20200904"), Some("20200909"), Some("1d"), Some("1d"))), None))
    assertThrows[FeathrConfigException](OfflineDateTimeUtils.createTimeIntervalFromDateParam(Some(DateParam(None, None, Some("-1d"), Some("1d"))), None))
    assertThrows[FeathrConfigException](OfflineDateTimeUtils.createTimeIntervalFromDateParam(Some(DateParam(None, None, Some("1d"), Some("-1d"))), None))
  }

  @Test(description = "test createTimeIntervalFromDateParam with absolute startDate and endDate")
  def createTimeIntervalFromAbsoluteDates(): Unit = {
    assertEquals(
      OfflineDateTimeUtils.createTimeIntervalFromDateParam(Some(DateParam(Some("20200904"), Some("20200909"), None, None)), None),
      createDailyInterval("2020-09-04", "2020-09-10"))
  }

  @Test(description = "test createTimeIntervalFromDateParam with absolute hourly startDate and endDate")
  def createTimeIntervalFromAbsoluteDatesInDiffFormat(): Unit = {
    assertEquals(
      OfflineDateTimeUtils.createTimeIntervalFromDateParam(Some(DateParam(Some("2020/09/04/00"), Some("2020/09/09/00"), None, None)), Some("yyyy/MM/dd/hh")),
      createHourlyInterval("2020-09-04-00", "2020-09-09-01"))
  }

  @Test(description = "test createTimeIntervalFromDateParam with relative dateOffset and numDays")
  def createTimeIntervalFromRelativeDates(): Unit = {
    val today = LocalDate.now(OfflineDateTimeUtils.DEFAULT_ZONE_ID).atStartOfDay(OfflineDateTimeUtils.DEFAULT_ZONE_ID)
    assertEquals(
      OfflineDateTimeUtils.createTimeIntervalFromDateParam(Some(DateParam(None, None, Some("1d"), Some("1d"))), None),
      new DateTimeInterval(today.minusDays(1), today))

    assertEquals(
      OfflineDateTimeUtils.createTimeIntervalFromDateParam(Some(DateParam(None, None, Some("1d"), Some("1d"))), None, Some("20200904")),
      createDailyInterval("2020-09-03", "2020-09-04"))
  }

  @Test(description = "test createTimeIntervalFromDateParam with relative dateOffset and numDays during daylight saving switch")
  def createTimeIntervalFromRelativeDatesDuringDaylightSaving(): Unit = {
    assertEquals(
      OfflineDateTimeUtils.createTimeIntervalFromDateParam(Some(DateParam(None, None, Some("1d"), Some("1d"))), None, Some("20210315")),
      createDailyInterval("2021-03-14", "2021-03-15"))
  }

  @Test(description = "test createTimeIntervalFromDateParam with relative dateOffset and numDays")
  def createTimeIntervalFromRelativeDatesInHours(): Unit = {
    val today =
      ZonedDateTime.now(OfflineDateTimeUtils.DEFAULT_ZONE_ID).truncatedTo(OfflineDateTimeUtils.dateTimeResolutionToChronoUnit(DateTimeResolution.HOURLY))
    assertEquals(
      OfflineDateTimeUtils.createTimeIntervalFromDateParam(Some(DateParam(None, None, Some("1h"), Some("1h"))), None),
      new DateTimeInterval(today.minusHours(1), today))

    assertEquals(
      OfflineDateTimeUtils.createTimeIntervalFromDateParam(Some(DateParam(None, None, Some("1h"), Some("1h"))), Some("yyyy/MM/dd/hh"), Some("2020/09/04/05")),
      createHourlyInterval("2020-09-04-04", "2020-09-04-05"))
  }

  @Test(description = "test CreateIntervalFromFeatureGenDateParam")
  def testCreateIntervalFromFeatureGenDateParam(): Unit = {
    assertEquals(
      OfflineDateTimeUtils.createIntervalFromFeatureGenDateParam(DateParam(Some("20200904"), Some("20200909"), None, None)),
      createDailyInterval("2020-09-04", "2020-09-09"))
    assertEquals(
      OfflineDateTimeUtils.createIntervalFromFeatureGenDateParam(DateParam(Some("20200904"), Some("20200904"), None, None)),
      createDailyInterval("2020-09-04", "2020-09-05"))
  }

  @Test(description = "test truncateEpochSecond")
  def testTruncateEpochSecond(): Unit = {
    val date = ZonedDateTime.of(2020, 9, 4, 0, 0, 0, 0, OfflineDateTimeUtils.DEFAULT_ZONE_ID)
    assertEquals(OfflineDateTimeUtils.truncateEpochSecond(date.plusMinutes(90).toEpochSecond, isDaily = true), date.toEpochSecond)
    assertEquals(OfflineDateTimeUtils.truncateEpochSecond(date.plusMinutes(90).toEpochSecond, isDaily = false), date.plusHours(1).toEpochSecond)
  }

  @Test(description = "test getDateTimeResolutionFromPattern")
  def testGetDateTimeResolutionFromPattern(): Unit = {
    assertEquals(OfflineDateTimeUtils.getDateTimeResolutionFromPattern("yyyy/MM/dd"), DateTimeResolution.DAILY)
    assertEquals(OfflineDateTimeUtils.getDateTimeResolutionFromPattern("yyyy/MM/dd/HH"), DateTimeResolution.HOURLY)
    assertEquals(OfflineDateTimeUtils.getDateTimeResolutionFromPattern("yyyy-MM-dd"), DateTimeResolution.DAILY)
    assertEquals(OfflineDateTimeUtils.getDateTimeResolutionFromPattern("yyyy-MM-dd-HH"), DateTimeResolution.HOURLY)
    assertEquals(OfflineDateTimeUtils.getDateTimeResolutionFromPattern("yyyy-MM-dd-00"), DateTimeResolution.DAILY)
    assertEquals(OfflineDateTimeUtils.getDateTimeResolutionFromPattern("yyyy/MM/dd/hh"), DateTimeResolution.HOURLY)
    assertEquals(OfflineDateTimeUtils.getDateTimeResolutionFromPattern("yyyy/MM/dd/KK"), DateTimeResolution.HOURLY)
    assertEquals(OfflineDateTimeUtils.getDateTimeResolutionFromPattern("yyyy/MM/dd/kk"), DateTimeResolution.HOURLY)
  }

  @Test(description = "test dateTimeResolutionToChronoUnit")
  def testDateTimeResolutionToChronoUnit(): Unit = {
    assertEquals(OfflineDateTimeUtils.dateTimeResolutionToChronoUnit(DateTimeResolution.DAILY), ChronoUnit.DAYS)
    assertEquals(OfflineDateTimeUtils.dateTimeResolutionToChronoUnit(DateTimeResolution.HOURLY), ChronoUnit.HOURS)
  }
}
