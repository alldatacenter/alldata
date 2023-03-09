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

package org.apache.griffin.measure.datasource.connector.batch

import scala.collection.mutable.{Map => MutableMap}
import scala.util._

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.elasticsearch.hadoop.cfg.ConfigurationOptions

import org.apache.griffin.measure.configuration.dqdefinition.DataConnectorParam
import org.apache.griffin.measure.context.TimeRange
import org.apache.griffin.measure.datasource.TimestampStorage
import org.apache.griffin.measure.utils.ParamUtil._

/**
 * A batch data connector for ElasticSearch source with read support for multiple indices.
 *
 * Supported Configurations:
 *  - filterExprs : [[Seq]] of string expressions that act as where conditions (row filters)
 *  - selectionExprs : [[Seq]] of string expressions that act as selection conditions (column filters)
 *  - options : [[Map]] of elasticsearch options. Refer to [[ConfigurationOptions]] for options
 *  - paths : [[Seq]] of elasticsearch paths (indexes) to read from
 *
 * Some defaults assumed by this connector (if not set) are as follows:
 *  - `es.nodes` in options is 'localhost',
 *  - `es.port` in options is 9200
 *  - filterExprs is empty list
 *  - selectionExprs is empty list
 *
 * Note:
 *  - When reading from multiple indices, the schemas are merged.
 *  - Selection expressions are applied first, then the filter expressions.
 *  - filterExprs/selectionExprs may be left empty if no filters are to be applied.
 */
case class ElasticSearchDataConnector(
    @transient sparkSession: SparkSession,
    dcParam: DataConnectorParam,
    timestampStorage: TimestampStorage)
    extends BatchDataConnector {

  final val ElasticSearchFormat: String = "es"
  final val Options: String = "options"

  final val Paths: String = "paths"
  final val FilterExprs: String = "filterExprs"
  final val SelectionExprs: String = "selectionExprs"

  val config: Map[String, Any] = dcParam.getConfig

  final val filterExprs: Seq[String] = config.getStringArr(FilterExprs)
  final val selectionExprs: Seq[String] = config.getStringArr(SelectionExprs)
  final val options: MutableMap[String, String] =
    MutableMap(config.getParamStringMap(Options, Map.empty).toSeq: _*)
  final val paths: String = config.getStringArr(Paths).map(_.trim).mkString(",") match {
    case s: String if s.isEmpty =>
      griffinLogger.error(s"Mandatory configuration '$Paths' is either empty or not defined.")
      throw new IllegalArgumentException()
    case s: String => s
  }

  override def data(ms: Long): (Option[DataFrame], TimeRange) = {
    val dfOpt = {
      val dfOpt = Try {
        val indexesDF = sparkSession.read
          .options(options)
          .format(ElasticSearchFormat)
          .load(paths)

        val df = {
          if (selectionExprs.nonEmpty) indexesDF.selectExpr(selectionExprs: _*)
          else indexesDF
        }

        filterExprs.foldLeft(df)((currentDf, expr) => currentDf.where(expr))
      }

      dfOpt match {
        case Success(_) =>
        case Failure(exception) =>
          griffinLogger.error("Error occurred while reading data set.", exception)
      }

      val preDfOpt = preProcess(dfOpt.toOption, ms)
      preDfOpt
    }

    (dfOpt, TimeRange(ms, readTmst(ms)))
  }
}
