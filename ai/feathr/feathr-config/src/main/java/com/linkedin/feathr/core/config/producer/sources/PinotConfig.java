package com.linkedin.feathr.core.config.producer.sources;

import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Represents the Pinot source config. For example
 *
 * "recentPageViewsSource": {
 *   type: "PINOT"
 *   resourceName: "recentMemberActionsPinotQuery"
 *   queryTemplate: "SELECT objectAttributes, timeStampSec
 *                   FROM RecentMemberActions
 *                   WHERE actorId IN (?) AND timeStampSec > ?
 *                   ORDER BY timeStampSec DESC
 *                   LIMIT 1000"
 *   queryArguments: ["key[0]", "System.currentTimeMillis()/1000 - 2 * 24 * 60 * 60"]
 *   queryKeyColumns: ["actorId"]
 * }
 */
public class PinotConfig extends SourceConfig {
  private final String _resourceName;
  private final String _queryTemplate;
  private final String[] _queryArguments;
  private final String[] _queryKeyColumns;

  /*
   * Fields to specify the Pinot source configuration
   */
  public static final String RESOURCE_NAME = "resourceName";
  public static final String QUERY_TEMPLATE = "queryTemplate";
  public static final String QUERY_ARGUMENTS = "queryArguments";
  public static final String QUERY_KEY_COLUMNS = "queryKeyColumns";

  /**
   * Constructor
   * @param sourceName the name of the source referenced by anchors in the feature definition
   * @param resourceName the service name in the Pinot D2 config for the queried Pinot table
   * @param queryTemplate the sql query template to fetch data from Pinot table, with “?” as placeholders for queryArguments replacement at runtime
   * @param queryArguments the array of key expression, whose element is used to replace the "?" in queryTemplate in the same order
   * @param queryKeyColumns the array of String for Pinot table column names that correspond to key argument defined queryArguments in the same order
   */
  public PinotConfig(@Nonnull String sourceName, @Nonnull String resourceName, @Nonnull String queryTemplate,
      @Nonnull String[] queryArguments, @Nonnull String[] queryKeyColumns) {
    super(sourceName);
    _resourceName = resourceName;
    _queryTemplate = queryTemplate;
    _queryArguments = queryArguments;
    _queryKeyColumns = queryKeyColumns;
  }

  public String getResourceName() {
    return _resourceName;
  }

  public String getQueryTemplate() {
    return _queryTemplate;
  }

  public String[] getQueryArguments() {
    return _queryArguments;
  }

  public String[] getQueryKeyColumns() {
    return _queryKeyColumns;
  }

  @Override
  public SourceType getSourceType() {
    return SourceType.PINOT;
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
    PinotConfig that = (PinotConfig) o;
    return Objects.equals(_resourceName, that._resourceName)
        && Objects.equals(_queryTemplate, that._queryTemplate)
        && Arrays.equals(_queryArguments, that._queryArguments)
        && Arrays.equals(_queryKeyColumns, that._queryKeyColumns);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(super.hashCode(), _resourceName, _queryTemplate);
    result = 31 * result + Arrays.hashCode(_queryArguments) + Arrays.hashCode(_queryKeyColumns);
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("PinotConfig{");
    sb.append("_resourceName='").append(_resourceName).append('\'');
    sb.append(", _queryTemplate='").append(_queryTemplate).append('\'');
    sb.append(", _queryArguments='").append(Arrays.toString(_queryArguments)).append('\'');
    sb.append(", _queryKeyColumns='").append(Arrays.toString(_queryKeyColumns)).append('\'');
    sb.append(", _sourceName='").append(_sourceName).append('\'');
    sb.append('}');
    return sb.toString();
  }
}