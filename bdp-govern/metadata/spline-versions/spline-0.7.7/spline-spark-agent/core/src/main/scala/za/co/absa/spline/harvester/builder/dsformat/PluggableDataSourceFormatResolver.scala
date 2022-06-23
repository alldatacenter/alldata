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

package za.co.absa.spline.harvester.builder.dsformat

import org.apache.spark.sql.sources.DataSourceRegister
import za.co.absa.spline.harvester.plugin.DataSourceFormatNameResolving
import za.co.absa.spline.harvester.plugin.registry.PluginRegistry

class PluggableDataSourceFormatResolver(pluginRegistry: PluginRegistry) extends DataSourceFormatResolver {

  private val processFn =
    pluginRegistry.plugins[DataSourceFormatNameResolving]
      .map(_.formatNameResolver)
      .reduce(_ orElse _)
      .orElse[AnyRef, String] {
        case dsr: DataSourceRegister => dsr.shortName
        case o => o.toString
      }

  override def resolve(o: AnyRef): String = processFn(o)
}
