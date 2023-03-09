package com.linkedin.feathr.offline.anchored

import com.linkedin.feathr.common.exception.FeathrConfigException
import com.linkedin.feathr.offline.TestFeathr
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * A unit test class for WindowTimeUnit.
 */
class TestWindowTimeUnit extends TestFeathr {

  /**
   * Unit test for parsing days.
   */
  @Test
  def testParseWindowTimeForDays(): Unit = {
    val dayVal = 5
    assertEquals(WindowTimeUnit.parseWindowTime(s"${dayVal}D").toDays, dayVal)
    assertEquals(WindowTimeUnit.parseWindowTime(s"${dayVal}d").toDays, dayVal)
  }

  /**
   * Unit test for parsing hours.
   */
  @Test
  def testParseWindowTimeForHours(): Unit = {
    val hourVal = 11
    assertEquals(WindowTimeUnit.parseWindowTime(s"${hourVal}H").toHours, hourVal)
    assertEquals(WindowTimeUnit.parseWindowTime(s"${hourVal}h").toHours, hourVal)
  }

  /**
   * Unit test for parsing minutes.
   */
  @Test
  def testParseWindowTimeForMinutes(): Unit = {
    val minuteVal = 55
    assertEquals(WindowTimeUnit.parseWindowTime(s"${minuteVal}M").toMinutes, minuteVal)
    assertEquals(WindowTimeUnit.parseWindowTime(s"${minuteVal}m").toMinutes, minuteVal)
  }

  /**
   * Unit test for parsing seconds.
   */
  @Test
  def testParseWindowTimeForSeconds(): Unit = {
    val secVal = 30
    assertEquals(WindowTimeUnit.parseWindowTime(s"${secVal}S").toMillis, secVal * 1000)
    assertEquals(WindowTimeUnit.parseWindowTime(s"${secVal}s").toMillis, secVal * 1000)
  }

  /**
   * Unit test for invalid character.
   */
  @Test(expectedExceptions = Array(classOf[FeathrConfigException]), expectedExceptionsMessageRegExp = ".*'window' field.*is not correctly set.*")
  def testParseForInvalidWindowTime(): Unit = {
    val secVal = 30
    assertEquals(WindowTimeUnit.parseWindowTime(s"${secVal}x").toMillis, secVal * 1000)
  }
}
