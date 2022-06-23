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

import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.catalyst.plans.logical._
import za.co.absa.commons.reflect.ReflectionUtils.extractFieldValue
import za.co.absa.commons.reflect.extractors.SafeTypeMatchingExtractor
import za.co.absa.spline.harvester.builder.SourceIdentifier
import za.co.absa.spline.harvester.plugin.Plugin.{Params, Precedence, ReadNodeInfo, WriteNodeInfo}
import za.co.absa.spline.harvester.plugin.embedded.DataSourceV2Plugin._
import za.co.absa.spline.harvester.plugin.{Plugin, ReadNodeProcessing, WriteNodeProcessing}

import java.net.URI
import javax.annotation.Priority
import scala.language.reflectiveCalls
import scala.util.Try

@Priority(Precedence.Normal)
class DataSourceV2Plugin
  extends Plugin
    with ReadNodeProcessing
    with WriteNodeProcessing {

  override val readNodeProcessor: PartialFunction[LogicalPlan, ReadNodeInfo] = {
    case `_: DataSourceV2Relation`(relation) =>
      val table = extractFieldValue[AnyRef](relation, "table")
      val tableName = extractFieldValue[String](table, "name")
      val identifier = extractFieldValue[AnyRef](relation, "identifier")
      val options =  extractFieldValue[AnyRef](relation, "options")
      val props = Map(
        "table" -> Map("identifier" -> tableName),
        "identifier" -> identifier,
        "options" -> options)
      (extractSourceIdFromTable(table), props)
  }

  override val writeNodeProcessor: PartialFunction[LogicalPlan, WriteNodeInfo] = {
    case `_: V2WriteCommand`(writeCommand) =>
      val namedRelation = extractFieldValue[AnyRef](writeCommand, "table")
      val query = extractFieldValue[LogicalPlan](writeCommand, "query")

      val tableName = extractFieldValue[AnyRef](namedRelation, "name")
      val output = extractFieldValue[AnyRef](namedRelation, "output")
      val writeOptions = extractFieldValue[Map[String, String]](writeCommand, "writeOptions")
      val isByName = extractFieldValue[Boolean](writeCommand, IsByName)

      val props = Map(
        "table" -> Map("identifier" -> tableName, "output" -> output),
        "writeOptions" -> writeOptions,
        IsByName -> isByName)

      val sourceId = extractSourceIdFromRelation(namedRelation)

      processV2WriteCommand(writeCommand, sourceId, query, props)

    case `_: CreateTableAsSelect`(ctc) =>
      val prop = "ignoreIfExists" -> extractFieldValue[Boolean](ctc, "ignoreIfExists")
      processV2CreateTableCommand(ctc,prop)

    case `_: ReplaceTableAsSelect`(ctc) =>
      val prop = "orCreate" -> extractFieldValue[Boolean](ctc, "orCreate")
      processV2CreateTableCommand(ctc, prop)
  }

  /**
    * @param v2WriteCommand org.apache.spark.sql.catalyst.plans.logical.V2WriteCommand
    */
  private def processV2WriteCommand(
    v2WriteCommand: AnyRef,
    sourceId: SourceIdentifier,
    query: LogicalPlan,
    props: Params
  ): (SourceIdentifier, SaveMode, LogicalPlan, Params) = v2WriteCommand match {
    case `_: AppendData`(_) =>
      (sourceId, SaveMode.Append, query, props)

    case `_: OverwriteByExpression`(obe) =>
      val deleteExpr = extractFieldValue[AnyRef](obe, "deleteExpr")
      (sourceId, SaveMode.Overwrite, query, props + ("deleteExpr" -> deleteExpr))

    case `_: OverwritePartitionsDynamic`(_) =>
      (sourceId, SaveMode.Overwrite, query, props)
  }

  private def processV2CreateTableCommand(
    ctc: AnyRef,
    commandSpecificProp: (String, _)
  ) : (SourceIdentifier, SaveMode, LogicalPlan, Params) = {
    val catalog = extractFieldValue[AnyRef](ctc, "catalog")
    val identifier = extractFieldValue[AnyRef](ctc, "tableName")
    val loadTableMethods = catalog.getClass.getMethods.filter(_.getName == "loadTable")
    val table = loadTableMethods.flatMap(m => Try(m.invoke(catalog, identifier)).toOption).head
    val sourceId = extractSourceIdFromTable(table)

    val query = extractFieldValue[LogicalPlan](ctc, "query")

    val partitioning = extractFieldValue[AnyRef](ctc, "partitioning")
    val properties = extractFieldValue[Map[String, String]](ctc, "properties")
    val writeOptions = extractFieldValue[Map[String, String]](ctc, "writeOptions")
    val props = Map(
      "table" -> Map("identifier" -> identifier.toString),
      "partitioning" -> partitioning,
      "properties" -> properties,
      "writeOptions" -> writeOptions)

    (sourceId, SaveMode.Overwrite, query, props + commandSpecificProp)
  }

  /**
    * @param namedRelation org.apache.spark.sql.catalyst.analysis.NamedRelation
    */
  private def extractSourceIdFromRelation(namedRelation: AnyRef): SourceIdentifier = {
    val table = extractFieldValue[AnyRef](namedRelation, "table")
    extractSourceIdFromTable(table)
  }

  /**
    * @param table org.apache.spark.sql.connector.catalog.Table
    */
  private def extractSourceIdFromTable(table: AnyRef): SourceIdentifier = table match {
    case `_: CassandraTable`(ct) =>
      val metadata = extractFieldValue[AnyRef](ct, "metadata")
      val keyspace = extractFieldValue[AnyRef](metadata, "keyspace")
      val name = extractFieldValue[AnyRef](metadata, "name")
      SourceIdentifier(Some("cassandra"), s"cassandra:$keyspace:$name")

    case `_: DatabricksDeltaTableV2`(dt) => extractSourceIdFromDeltaTableV2(dt)

    case `_: FileTable`(ft) =>
      val format = extractFieldValue[String](ft, "formatName").toLowerCase
      val paths = extractFieldValue[Seq[String]](ft, "paths")
      SourceIdentifier(Some(format), paths: _*)

    case `_: TableV2`(tv2) => extractSourceIdFromDeltaTableV2(tv2)
  }

  private def extractSourceIdFromDeltaTableV2(table: AnyRef): SourceIdentifier = {
    val tableProps = extractFieldValue[java.util.Map[String, String]](table, "properties")
    val location = tableProps.get("location")
    val uri =
      if(URI.create(location).getScheme == null) {
        s"file:$location"
      } else {
        location
      }
    val provider = tableProps.get("provider")
    SourceIdentifier(Some(provider), uri)
  }

}

object DataSourceV2Plugin {
  val IsByName = "isByName"

  object `_: V2WriteCommand` extends SafeTypeMatchingExtractor[AnyRef](
    "org.apache.spark.sql.catalyst.plans.logical.V2WriteCommand")

  object `_: AppendData` extends SafeTypeMatchingExtractor[AnyRef](
    "org.apache.spark.sql.catalyst.plans.logical.AppendData")

  object `_: OverwriteByExpression` extends SafeTypeMatchingExtractor[AnyRef](
    "org.apache.spark.sql.catalyst.plans.logical.OverwriteByExpression")

  object `_: OverwritePartitionsDynamic` extends SafeTypeMatchingExtractor[AnyRef](
    "org.apache.spark.sql.catalyst.plans.logical.OverwritePartitionsDynamic")

  object `_: CreateTableAsSelect` extends SafeTypeMatchingExtractor[AnyRef](
    "org.apache.spark.sql.catalyst.plans.logical.CreateTableAsSelect")

  object `_: ReplaceTableAsSelect` extends SafeTypeMatchingExtractor[AnyRef](
    "org.apache.spark.sql.catalyst.plans.logical.ReplaceTableAsSelect")

  object `_: DataSourceV2Relation` extends SafeTypeMatchingExtractor[AnyRef](
    "org.apache.spark.sql.execution.datasources.v2.DataSourceV2Relation")

  object `_: FileTable` extends SafeTypeMatchingExtractor[AnyRef](
    "org.apache.spark.sql.execution.datasources.v2.FileTable")

  object `_: DatabricksDeltaTableV2` extends SafeTypeMatchingExtractor[AnyRef](
    "com.databricks.sql.transaction.tahoe.catalog.DeltaTableV2")

  object `_: CassandraTable` extends SafeTypeMatchingExtractor[AnyRef](
    "com.datastax.spark.connector.datasource.CassandraTable")

  object `_: TableV2` extends SafeTypeMatchingExtractor[AnyRef](
    "org.apache.spark.sql.connector.catalog.Table")


}
