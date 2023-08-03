package com.linkedin.feathr.offline.util.datetime

import com.linkedin.feathr.common.DateTimeResolution
import com.linkedin.feathr.common.exception.FeathrConfigException
import org.scalatest.testng.TestNGSuite
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * A unit test class for DateTimePeriod.
 */
class TestDateTimePeriod extends TestNGSuite {

  /**
   * Unit test for parsing days.
   */
  @Test
  def testParseForDays(): Unit = {
    val days = DateTimePeriod.parse("1d")
    assertEquals(days.length, 1)
    assertEquals(days.unit, DateTimeResolution.DAILY)
  }

  /**
   * Unit test for parsing hours.
   */
  @Test
  def testParseForHours(): Unit = {
    val hours = DateTimePeriod.parse("2H")
    assertEquals(hours.length, 2)
    assertEquals(hours.unit, DateTimeResolution.HOURLY)
  }

  /**
   * Unit test for parsing string with spaces.
   */
  @Test
  def testParseWithSpaces(): Unit = {
    val hours = DateTimePeriod.parse(" 2 H ")
    assertEquals(hours.length, 2)
    assertEquals(hours.unit, DateTimeResolution.HOURLY)
  }

  /**
   * Unit test for invalid units.
   */
  @Test(expectedExceptions = Array(classOf[FeathrConfigException]))
  def testParseForInvalidTimeUnit(): Unit = {
    DateTimePeriod.parse("2m")
  }

  /**
   * Unit test for invalid length.
   */
  @Test(expectedExceptions = Array(classOf[FeathrConfigException]))
  def testParseForInvalidLength(): Unit = {
    DateTimePeriod.parse("1.5h")
  }

}
