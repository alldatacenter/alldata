package com.linkedin.feathr.offline.source.dataloader

import org.apache.spark.sql.SparkSession

/**
 * @param ss the spark session.
 */
private[offline] class JdbcDataLoaderFactory(ss: SparkSession) extends DataLoaderFactory {
  /**
   * create a data loader based on the file type.
   *
   * @param url the input file path
   * @return a [[DataLoader]]
   */
  override def create(url: String): DataLoader = {
    log.info(s"Creating JDBC data loader for url: ${url}")
    new JdbcDataLoader(ss, url)
  }
}
