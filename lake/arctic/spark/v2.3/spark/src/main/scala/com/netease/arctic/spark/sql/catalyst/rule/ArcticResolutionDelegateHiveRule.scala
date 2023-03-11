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

import com.netease.arctic.spark.source.ArcticSource
import com.netease.arctic.spark.sql.execution.{CreateArcticTableCommand, DropArcticTableCommand}
import com.netease.arctic.spark.sql.plan.{CreateArcticTableAsSelect, OverwriteArcticTableDynamic}
import org.apache.spark.sql.arctic.AnalysisException
import org.apache.spark.sql.catalyst.catalog.{CatalogTable, HiveTableRelation}
import org.apache.spark.sql.catalyst.expressions.{Alias, AttributeReference, Cast, Literal}
import org.apache.spark.sql.catalyst.plans.logical.{InsertIntoTable, LogicalPlan, Project}
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.execution.command.DDLUtils.HIVE_PROVIDER
import org.apache.spark.sql.execution.command.DropTableCommand
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Relation
import org.apache.spark.sql.execution.datasources.{CreateTable, LogicalRelation}
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.sources.v2.reader.DataSourceReader
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{SaveMode, SparkSession}

import java.util.Locale

case class ArcticResolutionDelegateHiveRule(spark: SparkSession) extends Rule[LogicalPlan] {

  lazy val arctic = new ArcticSource

  override def apply(plan: LogicalPlan): LogicalPlan = plan transformDown {
    // create table
    case CreateTable(tableDesc, mode, None)
      if tableDesc.provider.isDefined
        && tableDesc.provider.get.equalsIgnoreCase("arctic")
        && isDatasourceTable(tableDesc) =>
      CreateArcticTableCommand(arctic, tableDesc, ignoreIfExists = mode == SaveMode.Ignore)
    // create table as select
    case CreateTable(tableDesc, mode, Some(query))
      if tableDesc.provider.isDefined
        && tableDesc.provider.get.equalsIgnoreCase("arctic")
        && query.resolved && isDatasourceTable(tableDesc) =>
      val table = tableDesc.copy(schema = query.schema)
      CreateArcticTableAsSelect(arctic, table, query)

    // drop table
    case DropTableCommand(tableName, ifExists, isView, purge)
      if arctic.isDelegateDropTable(tableName, isView) =>
      DropArcticTableCommand(arctic, tableName, ifExists, purge)

    // insert into data source table
    case i@InsertIntoTable(l: LogicalRelation, _, _, _, _)
      if l.catalogTable.isDefined && arctic.isDelegateTable(l.catalogTable.get) =>
      createArcticInsert(i, l.catalogTable.get)

    // insert into hive table
    case i@InsertIntoTable(table: HiveTableRelation, _, _, _, _)
      if arctic.isDelegateTable(table.tableMeta) =>
      createArcticInsert(i, table.tableMeta)

    // scan datasource table
    case l@LogicalRelation(_, _, table, _) if table.isDefined && arctic.isDelegateTable(table.get) =>
      val reader = createArcticReader(table.get)
      val output = reader.readSchema()
        .map(f => AttributeReference(f.name, f.dataType, f.nullable, f.metadata)())
      DataSourceV2Relation(output, reader)

    // scan hive table
    case h@HiveTableRelation(tableMeta, dataCols, partitionCols)
      if arctic.isDelegateTable(tableMeta) =>
      val reader = createArcticReader(tableMeta)
      val output = reader.readSchema()
        .map(f => AttributeReference(f.name, f.dataType, f.nullable, f.metadata)())
      DataSourceV2Relation(output, reader)
  }

  def isDatasourceTable(table: CatalogTable): Boolean = {
    table.provider.isDefined && table.provider.get.toLowerCase(Locale.ROOT) != HIVE_PROVIDER
  }


  def createArcticInsert(i: InsertIntoTable, tableDesc: CatalogTable): LogicalPlan = {
    if (i.ifPartitionNotExists) {
      throw AnalysisException.message(
        s"Cannot write, IF NOT EXISTS is not supported for table: ${tableDesc.identifier}")
    }

    val table = arctic.loadTable(tableDesc.identifier)
    val query = addStaticPartitionColumns(table.schema(), i.partition, i.query)
    if (i.overwrite) {
      OverwriteArcticTableDynamic(i, table, query)
    } else {
      throw AnalysisException.message(
        s"Cannot write, INSERT INTO is not supported for table: ${tableDesc.identifier}")
    }
  }

  // add any static value as a literal column
  // part copied from spark-3.0 branch Analyzer.scala
  private def addStaticPartitionColumns(
                                         schema: StructType, partitionSpec: Map[String, Option[String]],
                                         query: LogicalPlan): LogicalPlan = {
    val staticPartitions = partitionSpec.filter(_._2.isDefined).mapValues(_.get)
    if (staticPartitions.isEmpty) {
      query
    } else {
      val output = schema.map(f => AttributeReference(f.name, f.dataType, f.nullable, f.metadata)())
      val conf = SQLConf.get
      val withStaticPartitionValues = {
        val outputNameToStaticName = staticPartitions.keySet.map(staticName =>
          output.find(col => conf.resolver(col.name, staticName)) match {
            case Some(attr) =>
              attr.name -> staticName
            case _ =>
              throw AnalysisException.message(
                s"Cannot add static value for unknown column: $staticName")
          }).toMap

        val queryColumns = query.output.iterator
        output.flatMap { col =>
          outputNameToStaticName.get(col.name).flatMap(staticPartitions.get) match {
            case Some(staticValue) =>
              // for each output column, add the static value as a literal, or use the next input
              // column. this does not fail if input columns are exhausted and adds remaining columns
              // at the end. both cases will be caught by ResolveOutputRelation and will fail the
              // query with a helpful error message.
              Some(Alias(Cast(Literal(staticValue), col.dataType), col.name)())
            case _ if queryColumns.hasNext =>
              Some(queryColumns.next)
            case _ =>
              None
          }
        } ++ queryColumns
      }

      Project(withStaticPartitionValues, query)
    }
  }

  def createArcticReader(table: CatalogTable): DataSourceReader = {
    val arcticTable = arctic.loadTable(table.identifier)
    val reader = arcticTable.createReader(null)
    reader
  }
}
