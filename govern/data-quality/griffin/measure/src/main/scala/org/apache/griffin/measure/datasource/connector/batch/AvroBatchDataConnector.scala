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

import org.apache.spark.sql.{DataFrame, SparkSession}

import org.apache.griffin.measure.configuration.dqdefinition.DataConnectorParam
import org.apache.griffin.measure.context.TimeRange
import org.apache.griffin.measure.datasource.TimestampStorage
import org.apache.griffin.measure.utils.HdfsUtil
import org.apache.griffin.measure.utils.ParamUtil._

/**
 * batch data connector for avro file
 */
@deprecated(
  s"This class is deprecated. Use '${classOf[FileBasedDataConnector].getCanonicalName}'.",
  "0.6.0")
case class AvroBatchDataConnector(
    @transient sparkSession: SparkSession,
    dcParam: DataConnectorParam,
    timestampStorage: TimestampStorage)
    extends BatchDataConnector {

  val config: Map[String, Any] = dcParam.getConfig

  val FilePath = "file.path"
  val FileName = "file.name"

  val filePath: String = config.getString(FilePath, "")
  val fileName: String = config.getString(FileName, "")

  val concreteFileFullPath: String = if (pathPrefix()) filePath else fileName

  private def pathPrefix(): Boolean = {
    filePath.nonEmpty
  }

  private def fileExist(): Boolean = {
    HdfsUtil.existPath(concreteFileFullPath)
  }

  def data(ms: Long): (Option[DataFrame], TimeRange) = {
    assert(fileExist(), s"Avro file $concreteFileFullPath is not exists!")
    val dfOpt = {
      val df = sparkSession.read.format("com.databricks.spark.avro").load(concreteFileFullPath)
      val dfOpt = Some(df)
      val preDfOpt = preProcess(dfOpt, ms)
      preDfOpt
    }
    val tmsts = readTmst(ms)
    (dfOpt, TimeRange(ms, tmsts))
  }

}
