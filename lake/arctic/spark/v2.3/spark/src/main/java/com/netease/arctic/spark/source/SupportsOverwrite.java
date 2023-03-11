package com.netease.arctic.spark.source;

import org.apache.spark.sql.sources.Filter;
import org.apache.spark.sql.sources.v2.writer.DataSourceWriter;

/**
 * mix-in interface for writer that support overwrite by filter.
 * Overwriting data by filter will delete any data that matches the filter
 * and replace it with data that is committed in the writer.
 */
public interface SupportsOverwrite extends DataSourceWriter {

  /**
   * Configures a writer to replace data matching the filters with data committed in the writer.
   * Rows must be deleted from the data source if and only if all filters are matched.
   * That is, filters must be interpreted as ANDed together.
   * @param filters – filters used to match data to overwrite
   * @return this writer builder for method chaining
   */
  DataSourceWriter overwrite(Filter[] filters);
}
