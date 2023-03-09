package com.linkedin.feathr.core.config.producer.sources;

import com.linkedin.feathr.core.config.ConfigObj;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;


/**
 * Base class to represent source configuration
 */
public abstract class SourceConfig implements ConfigObj {

  protected final String _sourceName;

  public static final String TYPE = "type";

  protected SourceConfig(@Nonnull String sourceName) {
    Validate.isTrue(StringUtils.isNotBlank(sourceName), "source name must not be blank!");
    _sourceName = sourceName;
  }

  public abstract SourceType getSourceType();

  /**
   * Returns the name associated with the source.
   * This is typically the name of the source as defined in the sources section of the feature definition file
   */
  public String getSourceName() {
    return _sourceName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SourceConfig that = (SourceConfig) o;
    return Objects.equals(_sourceName, that._sourceName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_sourceName);
  }
}
