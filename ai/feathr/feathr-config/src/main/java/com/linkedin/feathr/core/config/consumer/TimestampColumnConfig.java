package com.linkedin.feathr.core.config.consumer;
import com.linkedin.feathr.core.config.ConfigObj;
import java.util.Objects;


/**
 * Represents the timestamp column object
 *
 * @author rkashyap
 */
public class TimestampColumnConfig implements ConfigObj {
  public static final String NAME = "def";
  public static final String FORMAT = "format";

  private final String _name;
  private final String _format;

  private String _configStr;

  /**
   * Constructor
   * @param name name of the timestamp column
   * @param format format of the timestamp column
   */
  public TimestampColumnConfig(String name, String format) {
    _name = name;
    _format = format;

    constructConfigStr();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TimestampColumnConfig)) {
      return false;
    }
    TimestampColumnConfig that = (TimestampColumnConfig) o;
    return Objects.equals(_name, that._name) && Objects.equals(_format, that._format);
  }


  private void constructConfigStr() {
    StringBuilder sb = new StringBuilder();
    sb.append(NAME).append(": ").append(_name).append("\n")
        .append(FORMAT).append(": ").append(_format).append("\n");
    _configStr = sb.toString();
  }

  @Override
  public String toString() {
    return _configStr;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name, _format);
  }

  public String getName() {
    return _name;
  }

  public String getFormat() {
    return _format;
  }
}
