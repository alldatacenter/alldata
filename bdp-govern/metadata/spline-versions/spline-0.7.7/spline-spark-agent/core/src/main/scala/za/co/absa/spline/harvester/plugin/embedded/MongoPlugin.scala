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

import com.mongodb.spark.config.ReadConfig
import com.mongodb.spark.rdd.MongoRDD
import javax.annotation.Priority
import org.apache.spark.sql.execution.datasources.{LogicalRelation, SaveIntoDataSourceCommand}
import org.apache.spark.sql.sources.BaseRelation
import za.co.absa.commons.reflect.ReflectionUtils.extractFieldValue
import za.co.absa.commons.reflect.extractors.SafeTypeMatchingExtractor
import za.co.absa.spline.harvester.builder.SourceIdentifier
import za.co.absa.spline.harvester.plugin.Plugin.{Precedence, ReadNodeInfo, WriteNodeInfo}
import za.co.absa.spline.harvester.plugin.embedded.MongoPlugin._
import za.co.absa.spline.harvester.plugin.{BaseRelationProcessing, Plugin, RelationProviderProcessing}


@Priority(Precedence.Normal)
class MongoPlugin
  extends Plugin
    with BaseRelationProcessing
    with RelationProviderProcessing {

  import za.co.absa.commons.ExtractorImplicits._

  override def baseRelationProcessor: PartialFunction[(BaseRelation, LogicalRelation), ReadNodeInfo] = {
    case (`_: MongoRelation`(mongr), _) =>
      val mongoRDD = extractFieldValue[MongoRDD[_]](mongr, "mongoRDD")
      val readConfig = extractFieldValue[ReadConfig](mongoRDD, "readConfig")
      val database = readConfig.databaseName
      val collection = readConfig.collectionName
      val connectionUrl = readConfig.connectionString.getOrElse(sys.error("Unable to extract MongoDB connection URL"))
      (asSourceId(connectionUrl, database, collection), Map.empty)
  }

  override def relationProviderProcessor: PartialFunction[(AnyRef, SaveIntoDataSourceCommand), WriteNodeInfo] = {
    case (rp, cmd) if rp == "com.mongodb.spark.sql.DefaultSource" || MongoDBSourceExtractor.matches(rp) =>
      val database = cmd.options("database")
      val collection = cmd.options("collection")
      val uri = cmd.options("uri")
      (asSourceId(uri, database, collection), cmd.mode, cmd.query, cmd.options)
  }
}

object MongoPlugin {

  object `_: MongoRelation` extends SafeTypeMatchingExtractor[AnyRef]("com.mongodb.spark.sql.MongoRelation")

  private object MongoDBSourceExtractor extends SafeTypeMatchingExtractor(classOf[com.mongodb.spark.sql.DefaultSource])

  private def asSourceId(connectionUrl: String, database: String, collection: String) =
    SourceIdentifier(Some("mongodb"), s"$connectionUrl/$database.$collection")
}
