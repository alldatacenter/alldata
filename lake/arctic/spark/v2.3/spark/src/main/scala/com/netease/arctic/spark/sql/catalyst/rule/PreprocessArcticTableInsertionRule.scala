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

import com.netease.arctic.spark.source.ArcticSparkTable
import com.netease.arctic.spark.sql.plan.OverwriteArcticTableDynamic
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.arctic.AnalysisException
import org.apache.spark.sql.catalyst.catalog.HiveTableRelation
import org.apache.spark.sql.catalyst.optimizer.PropagateEmptyRelation.conf
import org.apache.spark.sql.catalyst.plans.logical.{InsertIntoTable, LogicalPlan}
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.execution.datasources.{DDLPreprocessingUtils, PartitioningUtils}


/**
 * process insert overwrite or insert into for arctic table.
 * 1. check query column size match with table schema.
 * 2. cast query column data type by projection to match table fields data type
 */
case class PreprocessArcticTableInsertionRule(spark: SparkSession) extends Rule[LogicalPlan] {
  override def apply(plan: LogicalPlan): LogicalPlan = plan transform {
    case a @ OverwriteArcticTableDynamic(i, table, query) if query.resolved =>
      val tableRelation = i.table
      tableRelation match {
        case relation: HiveTableRelation =>
          val metadata = relation.tableMeta
          val newQuery = process(i, table, query, metadata.partitionColumnNames)
          a.copy(query = newQuery)
        case _ => a
      }
  }


  def process(
      insert: InsertIntoTable,
      table: ArcticSparkTable,
      query: LogicalPlan,
      partColNames: Seq[String]): LogicalPlan = {

    val normalizedPartSpec = PartitioningUtils.normalizePartitionSpec(
      insert.partition, partColNames, table.name(), conf.resolver)

    val staticPartCols = normalizedPartSpec.filter(_._2.isDefined).keySet
    val expectedColumns = insert.table.output.filterNot(a => staticPartCols.contains(a.name))

    if (expectedColumns.length != query.schema.size) {
      throw AnalysisException.message(s"${table.name()} requires that the data to be inserted have the same number of columns as the " +
        s"target table: target table has ${insert.table.output.size} column(s) but the " +
        s"inserted data has ${insert.query.output.length + staticPartCols.size} column(s), " +
        s"including ${staticPartCols.size} partition column(s) having constant value(s).")
    }

    val newQuery = DDLPreprocessingUtils.castAndRenameQueryOutput(
      query, expectedColumns, conf)

    newQuery

  }
}
