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

package za.co.absa.spline.harvester.plugin.composite

import javax.annotation.Priority
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.execution.datasources.SaveIntoDataSourceCommand
import za.co.absa.commons.reflect.extractors.AccessorMethodValueExtractor
import za.co.absa.spline.harvester.builder.SourceIdentifier
import za.co.absa.spline.harvester.plugin.Plugin.{Precedence, WriteNodeInfo}
import za.co.absa.spline.harvester.plugin.composite.SaveIntoDataSourceCommandPlugin._
import za.co.absa.spline.harvester.plugin.registry.PluginRegistry
import za.co.absa.spline.harvester.plugin.{Plugin, RelationProviderProcessing, WriteNodeProcessing}
import za.co.absa.spline.harvester.qualifier.PathQualifier

@Priority(Precedence.Lowest)
class SaveIntoDataSourceCommandPlugin(
  pluginRegistry: PluginRegistry,
  pathQualifier: PathQualifier)
  extends Plugin
    with WriteNodeProcessing {

  private lazy val rpProcessor =
    pluginRegistry.plugins[RelationProviderProcessing]
      .map(_.relationProviderProcessor)
      .reduce(_ orElse _)


  override def writeNodeProcessor: PartialFunction[LogicalPlan, WriteNodeInfo] = {
    case cmd: SaveIntoDataSourceCommand => cmd match {
      case RelationProviderExtractor(rp)
        if rpProcessor.isDefinedAt((rp, cmd)) =>
        rpProcessor((rp, cmd))

      case _ =>
        val maybeProvider = RelationProviderExtractor.unapply(cmd)
        val opts = cmd.options
        val uri = opts.get("path").map(pathQualifier.qualify)
          .getOrElse(sys.error(s"Cannot extract source URI from the options: ${opts.keySet mkString ","}"))
        (SourceIdentifier(maybeProvider, uri), cmd.mode, cmd.query, opts)
    }
  }
}

object SaveIntoDataSourceCommandPlugin {

  private object RelationProviderExtractor extends AccessorMethodValueExtractor[AnyRef]("provider", "dataSource")

}
