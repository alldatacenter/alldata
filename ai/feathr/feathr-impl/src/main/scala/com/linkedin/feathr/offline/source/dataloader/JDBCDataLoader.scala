package com.linkedin.feathr.offline.source.dataloader

import com.linkedin.feathr.offline.source.dataloader.jdbc.JdbcUtils
import com.linkedin.feathr.offline.util.SourceUtils.processSanityCheckMode
import org.apache.avro.Schema
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * Load JDBC file for testing.
 * @param ss the spark session
 * @param url input resource path
 */
private[offline] class JdbcDataLoader(ss: SparkSession, url: String) extends DataLoader {
  override def loadSchema(): Schema = {
    ???
  }

  /**
   * load the source data as DataFrame
   */
  override def loadDataFrame(): DataFrame = {
    val df = JdbcUtils.loadDataFrame(ss, url)
    processSanityCheckMode(ss, df)
  }
}