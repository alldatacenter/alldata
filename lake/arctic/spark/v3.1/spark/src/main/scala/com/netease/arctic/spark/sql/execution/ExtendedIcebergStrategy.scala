/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.sql.execution

import com.netease.arctic.spark.{ArcticSparkCatalog, ArcticSparkSessionCatalog}
import org.apache.iceberg.spark.{Spark3Util, SparkCatalog, SparkSessionCatalog}
import org.apache.spark.sql.catalyst.analysis.NamedRelation
import org.apache.spark.sql.catalyst.expressions.{And, Expression, NamedExpression}
import org.apache.spark.sql.catalyst.planning.PhysicalOperation
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.connector.catalog.{Identifier, TableCatalog}
import org.apache.spark.sql.connector.iceberg.read.SupportsFileFilter
import org.apache.spark.sql.execution.datasources.v2._
import org.apache.spark.sql.execution.{FilterExec, LeafExecNode, ProjectExec, SparkPlan}
import org.apache.spark.sql.{SparkSession, Strategy}

import scala.collection.JavaConverters.seqAsJavaList

case class ExtendedIcebergStrategy(spark: SparkSession) extends Strategy {
  override def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
    case DynamicFileFilter(scanPlan, fileFilterPlan, filterable) =>
      DynamicFileFilterExec(planLater(scanPlan), planLater(fileFilterPlan), filterable) :: Nil

    case DynamicFileFilterWithCardinalityCheck(scanPlan, fileFilterPlan, filterable, filesAccumulator) =>
      DynamicFileFilterWithCardinalityCheckExec(
        planLater(scanPlan),
        planLater(fileFilterPlan),
        filterable,
        filesAccumulator) :: Nil

    case PhysicalOperation(project, filters, DataSourceV2ScanRelation(_, scan: SupportsFileFilter, output)) =>
      // projection and filters were already pushed down in the optimizer.
      // this uses PhysicalOperation to get the projection and ensure that if the batch scan does
      // not support columnar, a projection is added to convert the rows to UnsafeRow.
      val batchExec = ExtendedBatchScanExec(output, scan)
      withProjectAndFilter(project, filters, batchExec, !batchExec.supportsColumnar) :: Nil

    case ReplaceData(relation, batchWrite, query) =>
      ReplaceDataExec(batchWrite, refreshCache(relation), planLater(query)) :: Nil

    case MergeInto(mergeIntoParams, output, child) =>
      MergeIntoExec(mergeIntoParams, output, planLater(child)) :: Nil

    case _ =>
      Nil
  }

  private def withProjectAndFilter(
                                    project: Seq[NamedExpression],
                                    filters: Seq[Expression],
                                    scan: LeafExecNode,
                                    needsUnsafeConversion: Boolean): SparkPlan = {
    val filterCondition = filters.reduceLeftOption(And)
    val withFilter = filterCondition.map(FilterExec(_, scan)).getOrElse(scan)

    if (withFilter.output != project || needsUnsafeConversion) {
      ProjectExec(project, withFilter)
    } else {
      withFilter
    }
  }

  private def refreshCache(r: NamedRelation)(): Unit = {
    spark.sharedState.cacheManager.recacheByPlan(spark, r)
  }

  private object ArcticCatalogAndIdentifier {
    def unapply(identifier: Seq[String]): Option[(TableCatalog, Identifier)] = {
      val catalogAndIdentifier = Spark3Util.catalogAndIdentifier(spark, seqAsJavaList(identifier))
      catalogAndIdentifier.catalog() match {
        case arcticCatalog: ArcticSparkSessionCatalog[_] =>
          Some(arcticCatalog, catalogAndIdentifier.identifier())
        case arcticCatalog: ArcticSparkCatalog =>
          Some(arcticCatalog, catalogAndIdentifier.identifier())
        case icebergCatalog: SparkCatalog =>
          Some((icebergCatalog, catalogAndIdentifier.identifier))
        case icebergCatalog: SparkSessionCatalog[_] =>
          Some((icebergCatalog, catalogAndIdentifier.identifier))
        case _ =>
          None
      }
    }
  }
}
