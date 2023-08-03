package com.linkedin.feathr.offline.util.datetime

import com.linkedin.feathr.common.DateTimeResolution.DateTimeResolution

import java.time.{Instant, ZonedDateTime}
import java.util.Objects

/**
 * DateTimeInterval represents a time range with inclusive start and exclusive end.
 * Note the start time and end time are stored as timezone-insensitive Instant,
 * But they are returned in America/Los_Angeles timezone.
 *
 * Previously we were using Interval from Joda Time. But we used it as an [inclusive, inclusive] range that
 * the library is not designed for. We don't use joda time now because:
 * 1. it lacks some operations such as span and intersection.
 * 2. it's a deprecated library since Java 8.
 *
 * Another alternative is Interval from threeten-extra. But we don't want to introduce a 3rd-party library just for one class.
 * However, our implementation is highly influenced by that library.
 *
 * @param startInclusive inclusive start time
 * @param endExclusive exclusive end time
 */
private[offline] class DateTimeInterval(startInclusive: Instant, endExclusive: Instant) {

  private val start = startInclusive
  private val end = endExclusive

  /**
   * create interval with [[ZonedDateTime]]
   * @param startInclusive inclusive start time
   * @param endExclusive exclusive end time
   */
  def this(startInclusive: ZonedDateTime, endExclusive: ZonedDateTime) {
    this(startInclusive.toInstant, endExclusive.toInstant)
  }

  /**
   * get the inclusive start time as [[ZonedDateTime]] in America/Los_Angeles timezone.
   * @return the inclusive start time
   */
  def getStart: ZonedDateTime = start.atZone(OfflineDateTimeUtils.DEFAULT_ZONE_ID)

  /**
   * get the exclusive end time as [[ZonedDateTime]] in America/Los_Angeles timezone.
   * @return the exclusive end time
   */
  def getEnd: ZonedDateTime = end.atZone(OfflineDateTimeUtils.DEFAULT_ZONE_ID)

  /**
   * Checks if this interval overlaps the specified interval.
   * The result is true if the two intervals share some part of the time-line.
   * An empty interval overlaps itself.
   *
   * @param other the time interval to compare to
   * @return true if the time intervals overlap
   */
  def overlaps(other: DateTimeInterval): Boolean = {
    other.equals(this) || (start.compareTo(other.end) < 0 && other.start.compareTo(end) < 0)
  }

  /**
   * Calculates the smallest interval that encloses this interval and the specified interval.
   *
   * @param other  the other interval to check for, not null
   * @return the interval that spans the two intervals
   */
  def span(other: DateTimeInterval): DateTimeInterval = {
    val minStart = if (start.compareTo(other.start) <= 0) start else other.start
    val maxEnd = if (end.compareTo(other.end) >= 0) end else other.end
    new DateTimeInterval(minStart, maxEnd)
  }

  /**
   * Calculates the interval that is the intersection of this interval and the specified interval.
   *
   * This finds the intersection of two intervals.
   *
   * @param other  the other interval to check for
   * @return the interval that is the intersection of the two intervals
   */
  def intersection(other: DateTimeInterval): DateTimeInterval = {
    val maxStart = if (start.compareTo(other.start) >= 0) start else other.start
    val minEnd = if (end.compareTo(other.end) <= 0) end else other.end
    if (maxStart.compareTo(minEnd) < 0) {
      new DateTimeInterval(maxStart, minEnd)
    } else {
      new DateTimeInterval(maxStart, maxStart)
    }
  }

  /**
   * Calculates the interval that is the minimum and is able to cover this interval and the specified interval.
   *
   * @param other  the other interval to check for
   * @return the minimum interval that covers the two intervals
   */
  def minCoverage(other: DateTimeInterval): DateTimeInterval = {
    val minStart = if (start.compareTo(other.start) < 0) start else other.start
    val maxEnd = if (end.compareTo(other.end) > 0) end else other.end
    new DateTimeInterval(minStart, maxEnd)
  }

  /**
   * Checks if the range is empty.
   *
   * An empty range occurs when the start date equals the exclusive end date.
   *
   * @return true if the range is empty
   */
  def isEmpty(): Boolean = {
    start.compareTo(end) == 0
  }

  /**
   * return a new interval to align with the given date time resolution, in America/Los_Angeles timezone.
   * The start time is truncated and the end time is round up. So it may have a longer range than the original one.
   * If the original interval is empty, then it will also return an empty interval.
   * @param newResolution the date time resolution, either daily or hourly.
   * @return a new adjusted interval
   */
  def adjustWithDateTimeResolution(newResolution: DateTimeResolution): DateTimeInterval = {
    val newStart = DateTimeInterval.trim(getStart, newResolution, roundUp = false).toInstant
    if (isEmpty()) {
      new DateTimeInterval(newStart, newStart)
    } else {
      val newEnd = DateTimeInterval.trim(getEnd, newResolution, roundUp = true).toInstant
      new DateTimeInterval(newStart, newEnd)
    }
  }

  /**
   * Get all time point with the resolution(daily or hourly) within the interval.
   * @param resolution the date time resolution, either daily or hourly.
   * @return an array of all ZonedDateTime within the interval
   */
  def getAllTimeWithinInterval(resolution: DateTimeResolution): Array[ZonedDateTime] = {
    val adjusted = adjustWithDateTimeResolution(resolution)
    val chronoUnit = OfflineDateTimeUtils.dateTimeResolutionToChronoUnit(resolution)
    val length = chronoUnit.between(adjusted.getStart, adjusted.getEnd)
    val startTime = adjusted.getStart
    (0L until length).map(i => startTime.plus(i, chronoUnit)).toArray
  }

  /**
   * check whether this interval is equal to anther interval. i.e, they have the same start and end.
   * @param that the other object to check
   * @return true if this equals to that.
   */
  override def equals(that: Any): Boolean = {
    that match {
      case that: DateTimeInterval =>
        (this eq that) ||
          (start == that.start && end == that.end)
      case _ => false
    }
  }

  /**
   * generate hash code
   * @return hash code based on start and end.
   */
  override def hashCode(): Int = Objects.hash(start, end)

  /**
   * create a readable string
   * @return a readable string for the interval.
   */
  override def toString: String = {
    getStart.toString + "/" + getEnd.toString
  }
}

private[offline] object DateTimeInterval {

  /**
   * create an interval with both inclusive start and inclusive end Instant.
   * The end time will be extended to the beginning  of the next day/hour with the given date time resolution
   * @param startInclusive inclusive start time
   * @param endInclusive inclusive end time
   * @param dateTimeResolution date time resolution for extending the end time, either daily or hourly.
   * @return a new interval
   */
  def createFromInclusive(startInclusive: Instant, endInclusive: Instant, dateTimeResolution: DateTimeResolution): DateTimeInterval = {
    val endTimeInclusive = ZonedDateTime.ofInstant(endInclusive, OfflineDateTimeUtils.DEFAULT_ZONE_ID)
    val startTimeInclusive = ZonedDateTime.ofInstant(startInclusive, OfflineDateTimeUtils.DEFAULT_ZONE_ID)
    val endTimeExclusive = trim(endTimeInclusive, dateTimeResolution, false)
      .plus(1, OfflineDateTimeUtils.dateTimeResolutionToChronoUnit(dateTimeResolution))
    val zonedStartTimeInclusive = trim(startTimeInclusive, dateTimeResolution, false)
    new DateTimeInterval(zonedStartTimeInclusive.toInstant, endTimeExclusive.toInstant)
  }

  /**
   * create an interval with both inclusive start and inclusive end ZonedDateTime
   * The end time will be extended to the beginning  of the next day/hour with the given date time resolution
   * @param startInclusive inclusive start time
   * @param endInclusive inclusive end time
   * @param dateTimeResolution date time resolution for extending the end time, either daily or hourly.
   * @return a new interval
   */
  def createFromInclusive(startInclusive: ZonedDateTime, endInclusive: ZonedDateTime, dateTimeResolution: DateTimeResolution): DateTimeInterval = {
    createFromInclusive(startInclusive.toInstant, endInclusive.toInstant, dateTimeResolution)
  }

  /**
   * trim the input time to the beginning of the day/hour with the given date time resolution
   * @param dateTime the input time
   * @param dateTimeResolution date time resolution, either daily or hourly
   * @param roundUp if true, round up to a newer time; if false, round down to an older time
   * @return the trimmed ZonedDateTime
   */
  private def trim(dateTime: ZonedDateTime, dateTimeResolution: DateTimeResolution, roundUp: Boolean): ZonedDateTime = {
    val unit = OfflineDateTimeUtils.dateTimeResolutionToChronoUnit(dateTimeResolution)
    val trimmed = dateTime.truncatedTo(unit)
    if (roundUp && !trimmed.equals(dateTime)) {
      trimmed.plus(1, unit)
    } else {
      trimmed
    }
  }
}
