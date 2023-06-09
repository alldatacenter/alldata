package com.netease.arctic.spark.source;

import org.apache.spark.sql.sources.v2.writer.DataSourceWriter;

/**
 * Write builder trait for tables that support dynamic partition overwrite.
 */
public interface SupportsDynamicOverwrite extends DataSourceWriter {

  DataSourceWriter overwriteDynamicPartitions();
}
