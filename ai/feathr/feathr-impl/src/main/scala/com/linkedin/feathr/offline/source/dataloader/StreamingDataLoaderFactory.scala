package com.linkedin.feathr.offline.source.dataloader

import com.linkedin.feathr.offline.config.location.{DataLocation, KafkaEndpoint}
import com.linkedin.feathr.offline.source.dataloader.stream.KafkaDataLoader
import org.apache.spark.sql.SparkSession

/**
 * A data loader factory for streaming environment. In this case, it will always create a [[KafkaDataLoader]].
 *
 * @param ss the spark session.
 */
private[offline] class StreamingDataLoaderFactory(ss: SparkSession) extends  DataLoaderFactory {

  /**
   * create a data loader based on the location type
   *
   * @param input  the input location for streaming
   * @return a [[DataLoader]]
   */
  override def createFromLocation(input: DataLocation): DataLoader = new KafkaDataLoader(ss, input.asInstanceOf[KafkaEndpoint])

  /**
   * create a data loader based on the file type.
   *
   * @param path the input file path
   * @return a [[DataLoader]]
   */
  override def create(path: String): DataLoader = ???
}
