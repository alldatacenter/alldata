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
import za.co.absa.commons.reflect.ReflectionUtils.extractFieldValue
import za.co.absa.commons.reflect.extractors.SafeTypeMatchingExtractor
import za.co.absa.spline.harvester.builder.SourceIdentifier
import za.co.absa.spline.harvester.plugin.Plugin.{Precedence, ReadNodeInfo, WriteNodeInfo}
import za.co.absa.spline.harvester.plugin.embedded.CassandraPlugin._
import za.co.absa.spline.harvester.plugin.{BaseRelationProcessing, Plugin, RelationProviderProcessing}


@Priority(Precedence.Normal)
class CassandraPlugin
  extends Plugin
    with BaseRelationProcessing
    with RelationProviderProcessing {

  import za.co.absa.commons.ExtractorImplicits._

  override def baseRelationProcessor: PartialFunction[(BaseRelation, LogicalRelation), ReadNodeInfo] = {
    case (`_: CassandraSourceRelation`(casr), _) =>
      val tableRef = extractFieldValue[AnyRef](casr, "tableRef")
      val table = extractFieldValue[String](tableRef, "table")
      val keyspace = extractFieldValue[String](tableRef, "keyspace")
      (asSourceId(keyspace, table), Map.empty)
  }

  override def relationProviderProcessor: PartialFunction[(AnyRef, SaveIntoDataSourceCommand), WriteNodeInfo] = {
    case (rp, cmd) if rp == "org.apache.spark.sql.cassandra" || CassandraSourceExtractor.matches(rp) =>
      val keyspace = cmd.options("keyspace")
      val table = cmd.options("table")
      (asSourceId(keyspace, table), cmd.mode, cmd.query, cmd.options)
  }
}

object CassandraPlugin {

  object `_: CassandraSourceRelation` extends SafeTypeMatchingExtractor[AnyRef]("org.apache.spark.sql.cassandra.CassandraSourceRelation")

  private object CassandraSourceExtractor extends SafeTypeMatchingExtractor(classOf[org.apache.spark.sql.cassandra.DefaultSource])

  private def asSourceId(keyspace: String, table: String) = SourceIdentifier(Some("cassandra"), s"cassandra:$keyspace:$table")

}
