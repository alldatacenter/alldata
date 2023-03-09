package com.linkedin.feathr.offline.source.accessor

import com.linkedin.feathr.common.exception.FeathrException
import com.linkedin.feathr.offline.config.location.KafkaEndpoint
import com.linkedin.feathr.offline.source.DataSource
import com.linkedin.feathr.offline.source.dataloader.DataLoaderFactory
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * load a dataset from a streaming/kafka source.
 * @param ss the spark session
 * @param dataLoaderFactory loader for a single file
 * @param source          datasource
 */
private[offline] class StreamDataSourceAccessor(
    ss: SparkSession,
    dataLoaderFactory: DataLoaderFactory,
    source: DataSource)
    extends DataSourceAccessor(source) {

  /**
   * get source data as a dataframe
   *
   * @return the dataframe
   */
  override def get(): DataFrame = {
    if (!source.location.isInstanceOf[KafkaEndpoint]) {
      throw new FeathrException(s"${source.location} is not a Kakfa streaming source location." +
        s" Only Kafka source is supported right now.")
    }
    dataLoaderFactory.createFromLocation(source.location).loadDataFrame()
  }
}
