/*
 * Copyright 2020 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.harvester.plugin.embedded

import javax.annotation.Priority
import org.apache.spark.sql.SaveMode.{Append, Overwrite}
import org.apache.spark.sql.catalyst.catalog.{CatalogStorageFormat, HiveTableRelation}
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.execution.command.{CreateDataSourceTableAsSelectCommand, CreateTableCommand, DropTableCommand}
import org.apache.spark.sql.execution.datasources.{HadoopFsRelation, InsertIntoDataSourceCommand, InsertIntoHadoopFsRelationCommand, LogicalRelation}
import org.apache.spark.sql.hive.execution.{CreateHiveTableAsSelectCommand, InsertIntoHiveTable}
import org.apache.spark.sql.{SaveMode, SparkSession}
import za.co.absa.commons.reflect.extractors.SafeTypeMatchingExtractor
import za.co.absa.spline.harvester.builder._
import za.co.absa.spline.harvester.plugin.Plugin.{Precedence, ReadNodeInfo, WriteNodeInfo}
import za.co.absa.spline.harvester.plugin.embedded.SQLPlugin._
import za.co.absa.spline.harvester.plugin.extractor.CatalogTableExtractor
import za.co.absa.spline.harvester.plugin.{Plugin, ReadNodeProcessing, WriteNodeProcessing}
import za.co.absa.spline.harvester.qualifier.PathQualifier

import scala.language.reflectiveCalls

@Priority(Precedence.Normal)
class SQLPlugin(
  pathQualifier: PathQualifier,
  session: SparkSession)
  extends Plugin
    with ReadNodeProcessing
    with WriteNodeProcessing {

  private val extractor = new CatalogTableExtractor(session.catalog, pathQualifier)

  override val readNodeProcessor: PartialFunction[LogicalPlan, ReadNodeInfo] = {
    case htr: HiveTableRelation =>
      extractor.asTableRead(htr.tableMeta)

    case lr: LogicalRelation if lr.relation.isInstanceOf[HadoopFsRelation] =>
      val hr = lr.relation.asInstanceOf[HadoopFsRelation]
      lr.catalogTable
        .map(extractor.asTableRead)
        .getOrElse {
          val uris = hr.location.rootPaths.map(path => pathQualifier.qualify(path.toString))
          val fileFormat = hr.fileFormat
          (SourceIdentifier(Option(fileFormat), uris: _*), hr.options)
        }
  }

  override val writeNodeProcessor: PartialFunction[LogicalPlan, WriteNodeInfo] = {

    case cmd: InsertIntoHadoopFsRelationCommand if cmd.catalogTable.isDefined =>
      val catalogTable = cmd.catalogTable.get
      val mode = if (cmd.mode == SaveMode.Overwrite) Overwrite else Append
      extractor.asTableWrite(catalogTable, mode, cmd.query)

    case cmd: InsertIntoHadoopFsRelationCommand =>
      val path = cmd.outputPath.toString
      val qPath = pathQualifier.qualify(path)
      val format = cmd.fileFormat
      (SourceIdentifier(Option(format), qPath), cmd.mode, cmd.query, cmd.options)

    case cmd: InsertIntoDataSourceCommand =>
      val catalogTable = cmd.logicalRelation.catalogTable
      val path = catalogTable.flatMap(_.storage.locationUri).map(_.toString)
        .getOrElse(sys.error(s"Cannot extract source URI from InsertIntoDataSourceCommand"))
      val format = catalogTable.flatMap(_.provider).map(_.toLowerCase)
        .getOrElse(sys.error(s"Cannot extract format from InsertIntoDataSourceCommand"))
      val qPath = pathQualifier.qualify(path)
      val mode = if (cmd.overwrite) SaveMode.Overwrite else SaveMode.Append
      (SourceIdentifier(Some(format), qPath), mode, cmd.query, Map.empty)

    case `_: InsertIntoDataSourceDirCommand`(cmd) =>
      extractor.asDirWrite(cmd.storage, cmd.provider, cmd.overwrite, cmd.query)

    case `_: InsertIntoHiveDirCommand`(cmd) =>
      extractor.asDirWrite(cmd.storage, "hive", cmd.overwrite, cmd.query)

    case `_: InsertIntoHiveTable`(cmd) =>
      val mode = if (cmd.overwrite) Overwrite else Append
      extractor.asTableWrite(cmd.table, mode, cmd.query)

    case `_: CreateHiveTableAsSelectCommand`(cmd) =>
      val sourceId = extractor.asTableSourceId(cmd.tableDesc)
      (sourceId, cmd.mode, cmd.query, Map.empty)

    case cmd: CreateDataSourceTableAsSelectCommand =>
      extractor.asTableWrite(cmd.table, cmd.mode, cmd.query)

    case dtc: DropTableCommand =>
      val uri = extractor.asTableURI(dtc.tableName)
      val sourceId = SourceIdentifier(None, pathQualifier.qualify(uri))
      (sourceId, Overwrite, dtc, Map.empty)

    case ctc: CreateTableCommand =>
      val sourceId = extractor.asTableSourceId(ctc.table)
      (sourceId, Overwrite, ctc, Map.empty)
  }
}

object SQLPlugin {

  private object `_: InsertIntoHiveTable` extends SafeTypeMatchingExtractor(classOf[InsertIntoHiveTable])

  private object `_: CreateHiveTableAsSelectCommand` extends SafeTypeMatchingExtractor(
    classOf[CreateHiveTableAsSelectCommand])

  private object `_: InsertIntoHiveDirCommand` extends SafeTypeMatchingExtractor[InsertIntoHiveDirCommand](
    "org.apache.spark.sql.hive.execution.InsertIntoHiveDirCommand")

  private object `_: InsertIntoDataSourceDirCommand` extends SafeTypeMatchingExtractor[InsertIntoDataSourceDirCommand](
    "org.apache.spark.sql.execution.command.InsertIntoDataSourceDirCommand")

  private type InsertIntoHiveDirCommand = {
    def storage: CatalogStorageFormat
    def query: LogicalPlan
    def overwrite: Boolean
    def nodeName: String
  }

  private type InsertIntoDataSourceDirCommand = {
    def storage: CatalogStorageFormat
    def provider: String
    def query: LogicalPlan
    def overwrite: Boolean
    def nodeName: String
  }
}
