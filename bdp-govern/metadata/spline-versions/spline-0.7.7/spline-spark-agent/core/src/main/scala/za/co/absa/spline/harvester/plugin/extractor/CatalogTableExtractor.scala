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

package za.co.absa.spline.harvester.plugin.extractor
import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.SaveMode.{Append, Overwrite}
import org.apache.spark.sql.catalog.Catalog
import org.apache.spark.sql.catalyst.TableIdentifier
import org.apache.spark.sql.catalyst.catalog.{CatalogStorageFormat, CatalogTable}
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import za.co.absa.spline.harvester.builder.SourceIdentifier
import za.co.absa.spline.harvester.plugin.Plugin.{Params, WriteNodeInfo}
import za.co.absa.spline.harvester.qualifier.PathQualifier

class CatalogTableExtractor(catalog: Catalog, pathQualifier: PathQualifier) {

  def asTableURI(tableIdentifier: TableIdentifier): String = {
    val TableIdentifier(tableName, maybeTableDatabase) = tableIdentifier
    val databaseName = maybeTableDatabase getOrElse catalog.currentDatabase
    val databaseLocation = catalog.getDatabase(databaseName).locationUri.stripSuffix("/")
    s"$databaseLocation/${tableName.toLowerCase}"
  }

  def asTableSourceId(table: CatalogTable): SourceIdentifier = {
    val uri = table.storage.locationUri
      .map(_.toString)
      .getOrElse(asTableURI(table.identifier))
    SourceIdentifier(table.provider, pathQualifier.qualify(uri))
  }

  def asTableRead(ct: CatalogTable): (SourceIdentifier, Map[String, Any]) = {
    val sourceId = asTableSourceId(ct)
    val params = Map(
      "table" -> Map(
        "identifier" -> ct.identifier,
        "storage" -> ct.storage))
    (sourceId, params)
  }

  def asTableWrite(table: CatalogTable, mode: SaveMode, query: LogicalPlan): WriteNodeInfo = {
    val sourceIdentifier = asTableSourceId(table)
    (sourceIdentifier, mode, query, Map("table" -> Map("identifier" -> table.identifier, "storage" -> table.storage)))
  }

  def asDirWrite(storage: CatalogStorageFormat, provider: String, overwrite: Boolean, query: LogicalPlan): WriteNodeInfo = {
    val uri = storage.locationUri.getOrElse(sys.error(s"Cannot determine the data source location: $storage"))
    val mode = if (overwrite) Overwrite else Append
    (SourceIdentifier(Some(provider), uri.toString), mode, query, Map.empty: Params)
  }
}
