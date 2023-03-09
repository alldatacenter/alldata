package com.linkedin.feathr.offline.source.dataloader.stream

import com.linkedin.feathr.offline.source.dataloader.DataLoader
import org.apache.spark.sql._
import org.apache.spark.sql.streaming.DataStreamReader

// Interface for all streaming data loaders
abstract class StreamDataLoader(ss: SparkSession) extends DataLoader {
  lazy val _ss: SparkSession = ss

  def getDFReader(streamOptions: Map[String, String]): DataStreamReader
}



