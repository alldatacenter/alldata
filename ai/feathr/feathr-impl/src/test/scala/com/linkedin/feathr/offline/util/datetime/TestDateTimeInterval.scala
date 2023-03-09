package com.linkedin.feathr.offline.util.datetime

import com.linkedin.feathr.common.DateTimeResolution
import com.linkedin.feathr.offline.TestUtils.{createDailyInterval, createHourlyInterval}
import com.linkedin.feathr.offline.util.datetime.OfflineDateTimeUtils.createTimeFromString
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import org.testng.Assert.{assertEquals, assertFalse, assertTrue}

/**
 * unit tests for [[DateTimeInterval]]
 */
class TestDateTimeInterval extends TestNGSuite {

  @Test(description = "test creation of DateTimeInterval")
  def testCreation(): Unit = {
    val start = createTimeFromString("2020-09-04-07", "yyyy-MM-dd-HH")
    val end = createTimeFromString("2020-09-04-09", "yyyy-MM-dd-HH")

    val intervalFromInstant = new DateTimeInterval(start.toInstant, end.toInstant)
    assertEquals(intervalFromInstant.getStart, start)
    assertEquals(intervalFromInstant.getEnd, end)

    val intervalFromZonedDateTime = new DateTimeInterval(start, end)
    assertEquals(intervalFromZonedDateTime.getStart, start)
    assertEquals(intervalFromZonedDateTime.getEnd, end)

    val startUtc = createTimeFromString("2020-09-04-14", "yyyy-MM-dd-HH", "UTC")
    val endUtc = createTimeFromString("2020-09-04-16", "yyyy-MM-dd-HH", "UTC")
    val intervalFromUtc = new DateTimeInterval(startUtc, endUtc)
    assertEquals(intervalFromUtc.getStart, start)
    assertEquals(intervalFromUtc.getEnd, end)
  }

  @Test(description = "test create interval with inclusive daily end date")
  def testCreateFromInclusiveDaily(): Unit = {
    val start = createTimeFromString("2020-09-04", "yyyy-MM-dd")
    val end = createTimeFromString("2020-09-07", "yyyy-MM-dd")
    val interval = new DateTimeInterval(start, end.plusDays(1))

    assertEquals(DateTimeInterval.createFromInclusive(start, end, DateTimeResolution.DAILY), interval)
    assertEquals(DateTimeInterval.createFromInclusive(start, end.plusHours(5), DateTimeResolution.DAILY), interval)
  }

  @Test(description = "test create interval with inclusive hourly end date")
  def testCreateFromInclusiveHourly(): Unit = {
    val start = createTimeFromString("2020-09-04-07", "yyyy-MM-dd-HH")
    val end = createTimeFromString("2020-09-04-09", "yyyy-MM-dd-HH")
    val interval = new DateTimeInterval(start, end.plusHours(1))

    assertEquals(DateTimeInterval.createFromInclusive(start, end, DateTimeResolution.HOURLY), interval)
    assertEquals(DateTimeInterval.createFromInclusive(start, end.plusMinutes(5), DateTimeResolution.HOURLY), interval)
  }

  @Test(description = "test overlaps function")
  def testOverlaps(): Unit = {
    // first equals to second
    assertTrue(createDailyInterval("2020-09-04", "2020-09-07").overlaps(createDailyInterval("2020-09-04", "2020-09-07")))
    // first contains second
    assertTrue(createDailyInterval("2020-09-04", "2020-09-07").overlaps(createDailyInterval("2020-09-04", "2020-09-06")))
    assertTrue(createDailyInterval("2020-09-04", "2020-09-07").overlaps(createDailyInterval("2020-09-05", "2020-09-06")))
    assertTrue(createDailyInterval("2020-09-04", "2020-09-07").overlaps(createDailyInterval("2020-09-05", "2020-09-07")))
    // second contains first
    assertTrue(createDailyInterval("2020-09-04", "2020-09-06").overlaps(createDailyInterval("2020-09-04", "2020-09-07")))
    assertTrue(createDailyInterval("2020-09-05", "2020-09-06").overlaps(createDailyInterval("2020-09-04", "2020-09-07")))
    assertTrue(createDailyInterval("2020-09-05", "2020-09-07").overlaps(createDailyInterval("2020-09-04", "2020-09-07")))
    // second completely before second
    assertFalse(createDailyInterval("2020-09-04", "2020-09-07").overlaps(createDailyInterval("2020-09-02", "2020-09-03")))
    assertFalse(createDailyInterval("2020-09-04", "2020-09-07").overlaps(createDailyInterval("2020-09-02", "2020-09-04")))
    // second partly before first
    assertTrue(createDailyInterval("2020-09-04", "2020-09-07").overlaps(createDailyInterval("2020-09-02", "2020-09-05")))
    // second partly after first
    assertTrue(createDailyInterval("2020-09-04", "2020-09-07").overlaps(createDailyInterval("2020-09-05", "2020-09-09")))
    // second completely  after first
    assertFalse(createDailyInterval("2020-09-04", "2020-09-07").overlaps(createDailyInterval("2020-09-07", "2020-09-09")))
    assertFalse(createDailyInterval("2020-09-04", "2020-09-07").overlaps(createDailyInterval("2020-09-08", "2020-09-09")))
  }

  @Test(description = "test span function")
  def testSpan(): Unit = {
    // first equals to second
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").span(createDailyInterval("2020-09-04", "2020-09-07")),
      createDailyInterval("2020-09-04", "2020-09-07"))
    // first contains second
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").span(createDailyInterval("2020-09-04", "2020-09-06")),
      createDailyInterval("2020-09-04", "2020-09-07"))
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").span(createDailyInterval("2020-09-05", "2020-09-06")),
      createDailyInterval("2020-09-04", "2020-09-07"))
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").span(createDailyInterval("2020-09-05", "2020-09-07")),
      createDailyInterval("2020-09-04", "2020-09-07"))
    // second contains first
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-06").span(createDailyInterval("2020-09-04", "2020-09-07")),
      createDailyInterval("2020-09-04", "2020-09-07"))
    assertEquals(
      createDailyInterval("2020-09-05", "2020-09-06").span(createDailyInterval("2020-09-04", "2020-09-07")),
      createDailyInterval("2020-09-04", "2020-09-07"))
    assertEquals(
      createDailyInterval("2020-09-05", "2020-09-07").span(createDailyInterval("2020-09-04", "2020-09-07")),
      createDailyInterval("2020-09-04", "2020-09-07"))
    // second completely before second
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").span(createDailyInterval("2020-09-02", "2020-09-03")),
      createDailyInterval("2020-09-02", "2020-09-07"))
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").span(createDailyInterval("2020-09-02", "2020-09-04")),
      createDailyInterval("2020-09-02", "2020-09-07"))
    // second partly before first
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").span(createDailyInterval("2020-09-02", "2020-09-05")),
      createDailyInterval("2020-09-02", "2020-09-07"))
    // second partly after first
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").span(createDailyInterval("2020-09-05", "2020-09-09")),
      createDailyInterval("2020-09-04", "2020-09-09"))
    // second completely  after first
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").span(createDailyInterval("2020-09-07", "2020-09-09")),
      createDailyInterval("2020-09-04", "2020-09-09"))
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").span(createDailyInterval("2020-09-08", "2020-09-09")),
      createDailyInterval("2020-09-04", "2020-09-09"))
  }

  @Test(description = "test min coverage function")
  def testMinCoverage(): Unit = {
    // first equals to second
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").minCoverage(createDailyInterval("2020-09-04", "2020-09-07")),
      createDailyInterval("2020-09-04", "2020-09-07"))
    // second completely  after first
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-06").minCoverage(createDailyInterval("2020-09-07", "2020-09-09")),
      createDailyInterval("2020-09-04", "2020-09-09"))
  }

  @Test(description = "test intersection function")
  def testIntersection(): Unit = {
    // first equals to second
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").intersection(createDailyInterval("2020-09-04", "2020-09-07")),
      createDailyInterval("2020-09-04", "2020-09-07"))
    // first contains second
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").intersection(createDailyInterval("2020-09-04", "2020-09-06")),
      createDailyInterval("2020-09-04", "2020-09-06"))
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").intersection(createDailyInterval("2020-09-05", "2020-09-06")),
      createDailyInterval("2020-09-05", "2020-09-06"))
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").intersection(createDailyInterval("2020-09-05", "2020-09-07")),
      createDailyInterval("2020-09-05", "2020-09-07"))
    // second contains first
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-06").intersection(createDailyInterval("2020-09-04", "2020-09-07")),
      createDailyInterval("2020-09-04", "2020-09-06"))
    assertEquals(
      createDailyInterval("2020-09-05", "2020-09-06").intersection(createDailyInterval("2020-09-04", "2020-09-07")),
      createDailyInterval("2020-09-05", "2020-09-06"))
    assertEquals(
      createDailyInterval("2020-09-05", "2020-09-07").intersection(createDailyInterval("2020-09-04", "2020-09-07")),
      createDailyInterval("2020-09-05", "2020-09-07"))
    // second completely before second
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").intersection(createDailyInterval("2020-09-02", "2020-09-03")),
      createDailyInterval("2020-09-04", "2020-09-04"))
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").intersection(createDailyInterval("2020-09-02", "2020-09-04")),
      createDailyInterval("2020-09-04", "2020-09-04"))
    // second partly before first
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").intersection(createDailyInterval("2020-09-02", "2020-09-05")),
      createDailyInterval("2020-09-04", "2020-09-05"))
    // second partly after first
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").intersection(createDailyInterval("2020-09-05", "2020-09-09")),
      createDailyInterval("2020-09-05", "2020-09-07"))
    // second completely  after first
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").intersection(createDailyInterval("2020-09-07", "2020-09-09")),
      createDailyInterval("2020-09-07", "2020-09-07"))
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").intersection(createDailyInterval("2020-09-08", "2020-09-09")),
      createDailyInterval("2020-09-08", "2020-09-08"))
  }

  @Test(description = "test adjustWithDateTimeResolution with daily resolution")
  def testAdjustWithDateTimeResolutionDaily(): Unit = {
    val start = createTimeFromString("2020-09-04", "yyyy-MM-dd")
    val startWithMinute = start.plusMinutes(1)
    val end = createTimeFromString("2020-09-07", "yyyy-MM-dd")
    val endWithMinute = end.plusMinutes(1)

    assertEquals(new DateTimeInterval(start, end).adjustWithDateTimeResolution(DateTimeResolution.DAILY), new DateTimeInterval(start, end))
    assertEquals(new DateTimeInterval(startWithMinute, end).adjustWithDateTimeResolution(DateTimeResolution.DAILY), new DateTimeInterval(start, end))
    assertEquals(
      new DateTimeInterval(start, endWithMinute).adjustWithDateTimeResolution(DateTimeResolution.DAILY),
      new DateTimeInterval(start, end.plusDays(1)))
    assertEquals(
      new DateTimeInterval(startWithMinute, endWithMinute).adjustWithDateTimeResolution(DateTimeResolution.DAILY),
      new DateTimeInterval(start, end.plusDays(1)))
  }

  @Test(description = "test adjustWithDateTimeResolution with hourly resolution")
  def testAdjustWithDateTimeResolutionHourly(): Unit = {
    val start = createTimeFromString("2020-09-04-07", "yyyy-MM-dd-HH")
    val startWithMinute = start.plusMinutes(1)
    val end = createTimeFromString("2020-09-04-09", "yyyy-MM-dd-HH")
    val endWithMinute = end.plusMinutes(1)

    assertEquals(new DateTimeInterval(start, end).adjustWithDateTimeResolution(DateTimeResolution.HOURLY), new DateTimeInterval(start, end))
    assertEquals(new DateTimeInterval(startWithMinute, end).adjustWithDateTimeResolution(DateTimeResolution.HOURLY), new DateTimeInterval(start, end))
    assertEquals(
      new DateTimeInterval(start, endWithMinute).adjustWithDateTimeResolution(DateTimeResolution.HOURLY),
      new DateTimeInterval(start, end.plusHours(1)))
    assertEquals(
      new DateTimeInterval(startWithMinute, endWithMinute).adjustWithDateTimeResolution(DateTimeResolution.HOURLY),
      new DateTimeInterval(start, end.plusHours(1)))
  }

  @Test(description = "test getAllTimeWithinInterval with both daily and hourly resolution")
  def testGetAllTimeWithinInterval(): Unit = {
    val interval = createHourlyInterval("2020-09-04-07", "2020-09-04-09")
    assertEquals(
      interval.getAllTimeWithinInterval(DateTimeResolution.HOURLY),
      Array("2020-09-04-07", "2020-09-04-08").map(createTimeFromString(_, "yyyy-MM-dd-HH")))
    assertEquals(interval.getAllTimeWithinInterval(DateTimeResolution.DAILY), Array(createTimeFromString("2020-09-04", "yyyy-MM-dd")))
  }

  @Test(description = "test isEmpty")
  def testIsEmpty(): Unit = {
    assertTrue(createDailyInterval("2020-09-04", "2020-09-04").isEmpty())
    assertFalse(createDailyInterval("2020-09-04", "2020-09-07").isEmpty())
  }

  @Test(description = "test equals")
  def testEquals(): Unit = {
    assertTrue(createDailyInterval("2020-09-04", "2020-09-07").equals(createDailyInterval("2020-09-04", "2020-09-07")))
    assertFalse(createDailyInterval("2020-09-04", "2020-09-07").equals(createDailyInterval("2020-09-04", "2020-09-08")))
  }

  @Test(description = "test hashcode")
  def testHashCode(): Unit = {
    assertEquals(createDailyInterval("2020-09-04", "2020-09-07").hashCode(), createDailyInterval("2020-09-04", "2020-09-07").hashCode())
  }

  @Test(description = "test toString")
  def testToString(): Unit = {
    assertEquals(
      createDailyInterval("2020-09-04", "2020-09-07").toString,
      "2020-09-04T00:00-07:00[America/Los_Angeles]/2020-09-07T00:00-07:00[America/Los_Angeles]")
  }
}
