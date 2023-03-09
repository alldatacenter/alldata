package com.linkedin.feathr.core.config.producer.sources;

import java.util.Objects;
import java.util.Optional;


/**
 * Represents PassThrough source config
 */
public final class PassThroughConfig extends SourceConfig {
  private final String _dataModel;

  /**
   * Field used in PassThrough source config fragment
   */
  public static final String DATA_MODEL = "dataModel";

  /**
   * Constructor
   * @param sourceName the name of the source and it is referenced by the anchor in the feature definition
   * @param dataModel Class name for pass-through object
   */
  public PassThroughConfig(String sourceName, String dataModel) {
    super(sourceName);
    _dataModel = dataModel;
  }

  @Override
  public SourceType getSourceType() {
    return SourceType.PASSTHROUGH;
  }

  public Optional<String> getDataModel() {
    return Optional.ofNullable(_dataModel);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    PassThroughConfig that = (PassThroughConfig) o;
    return Objects.equals(_dataModel, that._dataModel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _dataModel);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("PassThroughConfig{");
    sb.append("_dataModel='").append(_dataModel).append('\'');
    sb.append(", _sourceName='").append(_sourceName).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
