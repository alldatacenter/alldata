/*
 * Copyright 2021 ABSA Group Limited
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

import io.github.classgraph.ClassGraph
import org.apache.hadoop.conf.Configuration
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.execution.datasources.{LogicalRelation, SaveIntoDataSourceCommand}
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.sources.BaseRelation
import org.apache.spark.sql.types.StructType
import za.co.absa.commons.reflect.ReflectionUtils.extractFieldValue
import za.co.absa.commons.reflect.extractors.SafeTypeMatchingExtractor
import za.co.absa.spline.harvester.builder.SourceIdentifier
import za.co.absa.spline.harvester.plugin.Plugin.{Precedence, ReadNodeInfo, WriteNodeInfo}
import za.co.absa.spline.harvester.plugin.embedded.BigQueryPlugin.SparkBigQueryConfig.ImmutableMap
import za.co.absa.spline.harvester.plugin.embedded.BigQueryPlugin._
import za.co.absa.spline.harvester.plugin.{BaseRelationProcessing, Plugin, RelationProviderProcessing}

import java.lang.reflect.Method
import java.lang.reflect.Modifier.isStatic
import java.util.Optional
import javax.annotation.Priority
import scala.collection.JavaConverters._
import scala.language.reflectiveCalls


@Priority(Precedence.Normal)
class BigQueryPlugin(spark: SparkSession)
  extends Plugin
    with BaseRelationProcessing
    with RelationProviderProcessing {

  import za.co.absa.commons.ExtractorImplicits._

  override def baseRelationProcessor: PartialFunction[(BaseRelation, LogicalRelation), ReadNodeInfo] = {
    case (`_: DirectBigQueryRelation`(bq), _) =>

      val tableId = extractFieldValue[AnyRef](bq, "tableId")
      val project = extractFieldValue[String](tableId, "project")
      val dataset = extractFieldValue[String](tableId, "dataset")
      val table = extractFieldValue[String](tableId, "table")

      (asSourceId(project, dataset, table), Map.empty)

  }

  override def relationProviderProcessor: PartialFunction[(AnyRef, SaveIntoDataSourceCommand), WriteNodeInfo] = {
    case (rp, cmd) if rp == "com.google.cloud.spark.bigquery" || BigQueryRelationProviderExtractor.matches(rp) =>

      def cmdOptionKeysStr = cmd.options.keys.mkString(", ")

      def bigQueryConfig = {
        SparkBigQueryConfig.from(
          ImmutableMap.copyOf(cmd.options.asJava),
          ImmutableMap.copyOf(spark.conf.getAll.asJava),
          spark.sparkContext.hadoopConfiguration,
          0,
          spark.sessionState.conf,
          spark.version,
          Optional.empty()
        )
      }

      val tableIdStr =
        cmd.options.get("path")
          .orElse(cmd.options.get("table"))
          .getOrElse(sys.error(s"Cannot find table info in the command. Options available: $cmdOptionKeysStr"))

      val tableId = TableId.parseTableId(tableIdStr)

      val table =
        Option(tableId.getTable)
          .getOrElse(sys.error("Table name cannot be `null`"))

      val dataset =
        Option(tableId.getDataset)
          .orElse(cmd.options.get("dataset"))
          .getOrElse(sys.error(s"Cannot find dataset info in the table ID ($tableIdStr) or in the command. Options available: $cmdOptionKeysStr"))

      val project =
        Option(tableId.getProject)
          .orElse(cmd.options.get("project"))
          .orElse(Option(bigQueryConfig.getParentProjectId))
          .getOrElse(sys.error(s"Cannot find project info in the table ID ($tableIdStr) or in the command. Options available: $cmdOptionKeysStr"))

      (asSourceId(project, dataset, table), cmd.mode, cmd.query, cmd.options)
  }
}

object BigQueryPlugin {

  //noinspection SameParameterValue
  private def findPossiblyShadedClass(packagePrefix: String, classNameSuffix: String): Class[_] = {
    new ClassGraph()
      .acceptPackages(packagePrefix)
      .scan()
      .getAllClasses
      .asScala
      .filter(_.getName.endsWith(classNameSuffix))
      .map(_.loadClass())
      .headOption
      .getOrElse(sys.error(s"Cannot find class `...$classNameSuffix` in package `$packagePrefix`"))
  }

  //
  // We cannot use the following static types here due to possible shading, so we'll mimic them.
  //

  object TableId {
    type TableId = {
      def getProject: String
      def getDataset: String
      def getTable: String
    }
    private val clazz = findPossiblyShadedClass("com.google.cloud", "com.google.cloud.bigquery.connector.common.BigQueryUtil")
    val parseTableId: String => TableId =
      clazz
        .getMethod("parseTableId", classOf[String])
        .invoke(clazz, _)
        .asInstanceOf[TableId]
  }

  object SparkBigQueryConfig {
    type SparkBigQueryConfig = {
      def getParentProjectId: String
    }
    private val clazz = findPossiblyShadedClass("com.google.cloud", "com.google.cloud.spark.bigquery.SparkBigQueryConfig")
    private val methodFrom: Method = clazz
      .getMethods
      .find(
        m => m.getName == "from"
          && isStatic(m.getModifiers)
          && m.getParameterTypes.length == 7
          && m.getReturnType.getSimpleName == "SparkBigQueryConfig"
      ).getOrElse(sys.error(s"Cannot find method `public static SparkBigQueryConfig from(... {7 args} ...)` in the class `$clazz`"))

    object ImmutableMap {
      type ImmutableMap = AnyRef
      private val imClass = SparkBigQueryConfig.methodFrom.getParameterTypes()(1) // 2nd parameter is `ImmutableMap`
      val copyOf: AnyRef => ImmutableMap =
        imClass
          .getMethod("copyOf", classOf[java.util.Map[_, _]])
          .invoke(imClass, _)
          .asInstanceOf[ImmutableMap]
    }

    val from: (ImmutableMap.ImmutableMap, ImmutableMap.ImmutableMap, Configuration, Integer, SQLConf, String, Optional[StructType]) => SparkBigQueryConfig =
      methodFrom
        .invoke(clazz, _, _, _, _, _, _, _)
        .asInstanceOf[SparkBigQueryConfig]
  }

  object `_: DirectBigQueryRelation` extends SafeTypeMatchingExtractor[AnyRef]("com.google.cloud.spark.bigquery.direct.DirectBigQueryRelation")

  private object BigQueryRelationProviderExtractor extends SafeTypeMatchingExtractor("com.google.cloud.spark.bigquery.BigQueryRelationProvider")

  private def asSourceId(project: String, dataset: String, table: String) = SourceIdentifier(Some("bigquery"), s"bigquery://$project:$dataset.$table")

}
