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
import org.apache.spark.sql.execution.datasources.{LogicalRelation, SaveIntoDataSourceCommand}
import org.apache.spark.sql.sources.BaseRelation
import org.elasticsearch.spark.cfg.SparkSettings
import za.co.absa.commons.reflect.ReflectionUtils.extractFieldValue
import za.co.absa.commons.reflect.extractors.SafeTypeMatchingExtractor
import za.co.absa.spline.harvester.builder.SourceIdentifier
import za.co.absa.spline.harvester.plugin.Plugin.{Precedence, ReadNodeInfo, WriteNodeInfo}
import za.co.absa.spline.harvester.plugin.embedded.ElasticSearchPlugin._
import za.co.absa.spline.harvester.plugin.{BaseRelationProcessing, Plugin, RelationProviderProcessing}


@Priority(Precedence.Normal)
class ElasticSearchPlugin
  extends Plugin
    with BaseRelationProcessing
    with RelationProviderProcessing {

  import za.co.absa.commons.ExtractorImplicits._

  override def baseRelationProcessor: PartialFunction[(BaseRelation, LogicalRelation), ReadNodeInfo] = {
    case (`_: ElasticsearchRelation`(esr), _) =>
      val parameters = extractFieldValue[SparkSettings](esr, "cfg")
      val server = parameters.getProperty("es.nodes")
      val indexDocType = parameters.getProperty("es.resource")
      (asSourceId(server, indexDocType), Map.empty)
  }

  override def relationProviderProcessor: PartialFunction[(AnyRef, SaveIntoDataSourceCommand), WriteNodeInfo] = {
    case (rp, cmd) if rp == "es" || ElasticSearchSourceExtractor.matches(rp) =>
      val indexDocType = cmd.options("path")
      val server = cmd.options("es.nodes")
      (asSourceId(server, indexDocType), cmd.mode, cmd.query, cmd.options)
  }
}

object ElasticSearchPlugin {

  object `_: ElasticsearchRelation` extends SafeTypeMatchingExtractor[AnyRef]("org.elasticsearch.spark.sql.ElasticsearchRelation")

  private object ElasticSearchSourceExtractor extends SafeTypeMatchingExtractor(classOf[org.elasticsearch.spark.sql.DefaultSource15])

  private def asSourceId(server: String, indexDocType: String) =
    SourceIdentifier(Some("elasticsearch"), s"elasticsearch://$server/$indexDocType")

}
