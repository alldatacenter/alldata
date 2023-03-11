/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.sql.catalyst.rule

import com.netease.arctic.spark.source.SupportsDynamicOverwrite
import com.netease.arctic.spark.sql.execution.CreateArcticTableAsSelectExec
import com.netease.arctic.spark.sql.plan.{CreateArcticTableAsSelect, OverwriteArcticTableDynamic}
import org.apache.spark.sql.arctic.AnalysisException
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.execution.SparkPlan
import org.apache.spark.sql.execution.datasources.v2.WriteToDataSourceV2Exec
import org.apache.spark.sql.{SaveMode, Strategy}

case class ArcticStrategies() extends Strategy {
  def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
    case OverwriteArcticTableDynamic(i, table, query) =>
      val optWriter = table.createWriter("", query.schema, SaveMode.Overwrite, null)
      if (!optWriter.isPresent) {
        throw AnalysisException.message(s"failed to create writer for table ${table.identifier}")
      }
      val writer = optWriter.get() match {
        case w: SupportsDynamicOverwrite => w.overwriteDynamicPartitions()
        case _ => throw AnalysisException.message(s"table ${table.identifier} does not support dynamic overwrite")
      }
      WriteToDataSourceV2Exec(writer, planLater(query)) :: Nil

    case CreateArcticTableAsSelect(arctic, tableDesc, query) =>
      CreateArcticTableAsSelectExec(arctic, tableDesc, planLater(query)) :: Nil
    case _ => Nil
  }


}
