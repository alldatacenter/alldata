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

package org.apache.griffin.measure.step.write

import scala.util.Try

import org.apache.commons.lang.StringUtils
import org.apache.spark.sql.DataFrame

import org.apache.griffin.measure.context.DQContext

/**
 * update data source streaming cache
 */
case class DataSourceUpdateWriteStep(dsName: String, inputName: String) extends WriteStep {

  val name: String = ""
  val writeTimestampOpt: Option[Long] = None

  def execute(context: DQContext): Try[Boolean] = Try {
    getDataSourceCacheUpdateDf(context) match {
      case Some(df) =>
        context.dataSources
          .find(ds => StringUtils.equals(ds.name, dsName))
          .foreach(_.updateData(df))
      case _ =>
        warn(s"update $dsName from $inputName fails")
    }
    true
  }

  private def getDataFrame(context: DQContext, name: String): Option[DataFrame] = {
    try {
      val df = context.sparkSession.table(s"`$name`")
      Some(df)
    } catch {
      case e: Throwable =>
        error(s"get data frame $name fails", e)
        None
    }
  }

  private def getDataSourceCacheUpdateDf(context: DQContext): Option[DataFrame] =
    getDataFrame(context, inputName)

}
