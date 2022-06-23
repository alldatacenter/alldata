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
import org.apache.spark.sql.catalyst.catalog.CatalogTable
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.{SaveMode, SparkSession}
import za.co.absa.commons.reflect.ReflectionUtils.extractFieldValue
import za.co.absa.commons.reflect.extractors.SafeTypeMatchingExtractor
import za.co.absa.spline.harvester.plugin.Plugin.{Precedence, WriteNodeInfo}
import za.co.absa.spline.harvester.plugin.embedded.DatabricksPlugin.`_: DataBricksCreateDeltaTableCommand`
import za.co.absa.spline.harvester.plugin.extractor.CatalogTableExtractor
import za.co.absa.spline.harvester.plugin.{Plugin, ReadNodeProcessing, WriteNodeProcessing}
import za.co.absa.spline.harvester.qualifier.PathQualifier

import scala.language.reflectiveCalls

@Priority(Precedence.Normal)
class DatabricksPlugin(
  pathQualifier: PathQualifier,
  session: SparkSession)
  extends Plugin
    with WriteNodeProcessing {

  private val extractor = new CatalogTableExtractor(session.catalog, pathQualifier)

  override val writeNodeProcessor: PartialFunction[LogicalPlan, WriteNodeInfo] = {
    case `_: DataBricksCreateDeltaTableCommand`(command) =>
      val table = extractFieldValue[CatalogTable](command, "table")
      val saveMode = extractFieldValue[SaveMode](command, "mode")
      val query = extractFieldValue[Option[LogicalPlan]](command, "query").get
      extractor.asTableWrite(table, saveMode, query)
  }
}

object DatabricksPlugin {

  private object `_: DataBricksCreateDeltaTableCommand` extends SafeTypeMatchingExtractor[AnyRef](
    "com.databricks.sql.transaction.tahoe.commands.CreateDeltaTableCommand")
}
