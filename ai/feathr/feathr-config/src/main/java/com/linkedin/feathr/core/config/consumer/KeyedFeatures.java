package com.linkedin.feathr.core.config.consumer;

import com.linkedin.feathr.core.utils.Utils;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * Represents tuple of key (which may be a multi or composite key), and the list of features specific to this key.
 *
 * @author djaising
 * @author cesun
 */
public final class KeyedFeatures {

  /*
   * Represents the fields used to specify the key, features, and temporal parameters in the Join Config file.
   */
  public static final String KEY = "key";
  public static final String FEATURE_LIST = "featureList";
  public static final String START_DATE = "startDate";
  public static final String END_DATE = "endDate";
  public static final String DATE_OFFSET = "dateOffset";  // TODO: verify field name
  public static final String NUM_DAYS = "numDays";        // TODO: verify field name
  public static final String OVERRIDE_TIME_DELAY = "overrideTimeDelay";

  // Not a field but is used to specify the timestamp format
  public static final String TIMESTAMP_FORMAT = "yyyyMMdd";

  private final List<String> _key;
  private final List<String> _features;
  private final Optional<DateTimeRange> _dates;
  private final Optional<Duration> _overrideTimeDelay;

  private String _configStr;

  /**
   * Constructor with all parameters
   * @param key If the list contains multiple entries, it specifies a composite key else a single key.
   * @param features List of features specific to the key.
   * @param dates {@link DateTimeRange} object which delimits the start and end times of the feature records to be
   *              fetched.
   */
  public KeyedFeatures(List<String> key, List<String> features, DateTimeRange dates, Duration overrideTimeDelay) {
    _key = key;
    _features = features;
    _dates = Optional.ofNullable(dates);
    _overrideTimeDelay = Optional.ofNullable(overrideTimeDelay);
    constructConfigStr();
  }

  private void constructConfigStr() {
    StringBuilder sb = new StringBuilder();
    sb.append(KEY).append(": ").append(Utils.string(_key)).append("\n")
        .append(FEATURE_LIST).append(": ").append(Utils.string(_features)).append("\n");
    _dates.ifPresent(d -> sb.append(START_DATE).append(": ").append(d.getStart()).append("\n")
        .append(END_DATE).append(": ").append(d.getEnd()).append("\n"));
    _overrideTimeDelay.ifPresent(d -> sb.append(OVERRIDE_TIME_DELAY).append(": ").append(d).append("\n"));
    _configStr = sb.toString();
  }

  @Override
  public String toString() {
    return _configStr;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof KeyedFeatures)) {
      return false;
    }
    KeyedFeatures that = (KeyedFeatures) o;
    return Objects.equals(_key, that._key) && Objects.equals(_features, that._features) && Objects.equals(_dates,
        that._dates) && Objects.equals(_overrideTimeDelay, that._overrideTimeDelay);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_key, _features, _dates, _overrideTimeDelay);
  }

  public List<String> getKey() {
    return _key;
  }

  public List<String> getFeatures() {
    return _features;
  }

  public Optional<DateTimeRange> getDates() {
    return _dates;
  }

  public Optional<Duration> getOverrideTimeDelay() {
    return _overrideTimeDelay; }

}
