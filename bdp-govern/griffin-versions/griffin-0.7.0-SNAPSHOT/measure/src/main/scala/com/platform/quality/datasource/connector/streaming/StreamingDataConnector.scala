/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.platform.quality.datasource.connector.streaming

import com.platform.quality.context.TimeRange
import com.platform.quality.datasource.cache.StreamingCacheClient
import com.platform.quality.datasource.connector.DataConnector

import scala.util.Try
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.streaming.dstream.InputDStream

trait StreamingDataConnector extends DataConnector {

  type K
  type V
  type OUT

  protected def stream(): Try[InputDStream[OUT]]

  // transform rdd to dataframe
  def transform(rdd: RDD[OUT]): Option[DataFrame]

  // streaming data connector cannot directly read data frame
  def data(ms: Long): (Option[DataFrame], TimeRange) = (None, TimeRange.emptyTimeRange)

  val streamingCacheClientOpt: Option[StreamingCacheClient]

}
