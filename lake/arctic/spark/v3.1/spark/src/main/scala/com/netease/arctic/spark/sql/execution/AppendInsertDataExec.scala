/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.sql.execution

import com.netease.arctic.spark.table.ArcticSparkTable
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.execution.SparkPlan
import org.apache.spark.sql.execution.datasources.v2.{ArcticTableWriteExec, BatchWriteHelper}
import org.apache.spark.sql.util.CaseInsensitiveStringMap

case class AppendInsertDataExec(table: ArcticSparkTable,
                                writeOptions: CaseInsensitiveStringMap,
                                queryInsert: SparkPlan,
                                validateQuery: SparkPlan,
                                refreshCache: () => Unit) extends ArcticTableWriteExec with BatchWriteHelper {
  override protected def run(): Seq[InternalRow] = {
    validateData()
    val writtenRows = writeInsert(newWriteBuilder().buildForBatch())
    refreshCache()
    writtenRows
  }


  override def query: SparkPlan = queryInsert

  override def left: SparkPlan = queryInsert

  override def right: SparkPlan = validateQuery
}
