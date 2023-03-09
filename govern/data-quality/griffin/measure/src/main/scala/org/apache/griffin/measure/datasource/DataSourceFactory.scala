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

package org.apache.griffin.measure.datasource

import scala.util._

import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.StreamingContext

import org.apache.griffin.measure.Loggable
import org.apache.griffin.measure.configuration.dqdefinition.DataSourceParam
import org.apache.griffin.measure.datasource.cache.StreamingCacheClientFactory
import org.apache.griffin.measure.datasource.connector.DataConnectorFactory

object DataSourceFactory extends Loggable {

  def getDataSources(
      sparkSession: SparkSession,
      ssc: StreamingContext,
      dataSources: Seq[DataSourceParam]): Seq[DataSource] = {
    dataSources.zipWithIndex.flatMap { pair =>
      val (param, index) = pair
      getDataSource(sparkSession, ssc, param, index)
    }
  }

  private def getDataSource(
      sparkSession: SparkSession,
      ssc: StreamingContext,
      dataSourceParam: DataSourceParam,
      index: Int): Option[DataSource] = {
    val name = dataSourceParam.getName
    val timestampStorage = TimestampStorage()

    // for streaming data cache
    val streamingCacheClientOpt = StreamingCacheClientFactory.getClientOpt(
      sparkSession,
      dataSourceParam.getCheckpointOpt,
      name,
      index,
      timestampStorage)

    val connectorParamsOpt = dataSourceParam.getConnector

    connectorParamsOpt match {
      case Some(connectorParam) =>
        val dataConnectors = DataConnectorFactory.getDataConnector(
          sparkSession,
          ssc,
          connectorParam,
          timestampStorage,
          streamingCacheClientOpt) match {
          case Success(connector) => Some(connector)
          case Failure(exception) =>
            error("Error initializing data source with name '$name'.", exception)
            throw exception
        }

        Some(DataSource(name, dataSourceParam, dataConnectors, streamingCacheClientOpt))
      case None =>
        error(s"Connector for data source with name '$name' is invalid.")
        throw new IllegalStateException()
    }
  }

}
