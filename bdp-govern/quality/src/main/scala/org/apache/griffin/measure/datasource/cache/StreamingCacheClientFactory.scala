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

package org.apache.griffin.measure.datasource.cache

import scala.util.matching.Regex

import org.apache.spark.sql.SparkSession

import org.apache.griffin.measure.Loggable
import org.apache.griffin.measure.datasource.TimestampStorage
import org.apache.griffin.measure.utils.ParamUtil._

object StreamingCacheClientFactory extends Loggable {

  private object DataSourceCacheType {
    val ParquetRegex: Regex = "^(?i)parq(uet)?$".r
    val JsonRegex: Regex = "^(?i)json$".r
    val OrcRegex: Regex = "^(?i)orc$".r
  }
  import DataSourceCacheType._

  val _type = "type"

  /**
   * create streaming cache client
   * @param sparkSession     sparkSession in spark environment
   * @param checkpointOpt  data source checkpoint/cache config option
   * @param name           data source name
   * @param index          data source index
   * @param tmstCache      the same tmstCache instance inside a data source
   * @return               streaming cache client option
   */
  def getClientOpt(
      sparkSession: SparkSession,
      checkpointOpt: Option[Map[String, Any]],
      name: String,
      index: Int,
      tmstCache: TimestampStorage): Option[StreamingCacheClient] = {
    checkpointOpt.flatMap { param =>
      try {
        val tp = param.getString(_type, "")
        val dsCache = tp match {
          case ParquetRegex() =>
            StreamingCacheParquetClient(sparkSession, param, name, index, tmstCache)
          case JsonRegex() =>
            StreamingCacheJsonClient(sparkSession, param, name, index, tmstCache)
          case OrcRegex() =>
            StreamingCacheOrcClient(sparkSession, param, name, index, tmstCache)
          case _ =>
            StreamingCacheParquetClient(sparkSession, param, name, index, tmstCache)
        }
        Some(dsCache)
      } catch {
        case _: Throwable =>
          error("generate data source cache fails")
          None
      }
    }
  }

}
