package com.linkedin.feathr.core.config.common;

import com.linkedin.feathr.core.config.ConfigObj;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.TimeZone;


/**
 * Represent a time period or a time point.
 * the startTime is <referenceEndDateTime in timeZone> - offset - length + 1,
 * the endTime is referenceEndDateTime in timeZone - offset
 */
public class DateTimeConfig implements ConfigObj {
  // end time of this time period, it is called reference because it might
  // need to shift by _offsetInSeconds to be the actual endTime, e.g., a date, or NOW, or LATEST
  private final String _referenceEndTime;
  // _referenceEndTime format, e.g., yyyy-MM-dd
  private final String _referenceEndTimeFormat;
  // daily or hourly
  private final ChronoUnit _timeResolution;
  // length of the time period, in terms of _timeResolution
  private final long _length;
  // offset of referenceEndTIme, means the actual end time is <_offset> before referenceEndTIme
  private final Duration _offset;
  private final TimeZone _timeZone;

  /**
   * Constructor
   * @param referenceEndTime end time of this time period, it is called reference because it might
   *                         need to shift by _offsetInSeconds to be the actual endTime, e.g., a date, or NOW, or LATEST
   * @param referenceEndTimeFormat format, e.g., yyyy-MM-dd
   * @param timeResolution daily or hourly
   * @param length length of the time period, in terms of _timeResolution
   * @param offset offset
   * @param timeZone time zone
   */
  public DateTimeConfig(String referenceEndTime, String referenceEndTimeFormat, ChronoUnit timeResolution, long length,
      Duration offset, TimeZone timeZone) {
    _referenceEndTime = referenceEndTime;
    _referenceEndTimeFormat = referenceEndTimeFormat;
    _timeResolution = timeResolution;
    _length = length;
    _offset = offset;
    _timeZone = timeZone;
  }

  /*
   * The previously used lombok library auto generates getters with underscore, which is used in production.
   * For backward compatibility, we need to keep these getters.
   * However, function name with underscore can not pass LinkedIn's style check, here we need suppress the style check
   *  for the getters only.
   *
   * For more detail, please refer to the style check wiki:
   * https://iwww.corp.linkedin.com/wiki/cf/display/TOOLS/Checking+Java+Coding+Style+with+Gradle+Checkstyle+Plugin
   *
   * TODO - 7493) remove the ill-named getters
   */
  // CHECKSTYLE:OFF
  @Deprecated
  public String get_referenceEndTime() {
    return _referenceEndTime;
  }

  @Deprecated
  public String get_referenceEndTimeFormat() {
    return _referenceEndTimeFormat;
  }

  @Deprecated
  public ChronoUnit get_timeResolution() {
    return _timeResolution;
  }

  @Deprecated
  public long get_length() {
    return _length;
  }

  @Deprecated
  public Duration get_offset() {
    return _offset;
  }

  @Deprecated
  public TimeZone get_timeZone() {
    return _timeZone;
  }
  // CHECKSTYLE:ON

  public String getReferenceEndTime() {
    return _referenceEndTime;
  }

  public String getReferenceEndTimeFormat() {
    return _referenceEndTimeFormat;
  }

  public ChronoUnit getTimeResolution() {
    return _timeResolution;
  }

  public long getLength() {
    return _length;
  }

  public Duration getOffset() {
    return _offset;
  }

  public TimeZone getTimeZone() {
    return _timeZone;
  }

  @Override
  public String toString() {
    return "DateTimeConfig{" + "_referenceEndTime='" + _referenceEndTime + '\'' + ", _referenceEndTimeFormat='"
        + _referenceEndTimeFormat + '\'' + ", _timeResolution=" + _timeResolution + ", _length=" + _length
        + ", _offset=" + _offset + ", _timeZone=" + _timeZone + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DateTimeConfig)) {
      return false;
    }
    DateTimeConfig that = (DateTimeConfig) o;
    return _length == that._length && Objects.equals(_referenceEndTime, that._referenceEndTime) && Objects.equals(
        _referenceEndTimeFormat, that._referenceEndTimeFormat) && _timeResolution == that._timeResolution
        && Objects.equals(_offset, that._offset) && Objects.equals(_timeZone, that._timeZone);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_referenceEndTime, _referenceEndTimeFormat, _timeResolution, _length, _offset, _timeZone);
  }
}