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

package za.co.absa.spline.harvester.plugin

import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.execution.datasources.{LogicalRelation, SaveIntoDataSourceCommand}
import org.apache.spark.sql.sources.{BaseRelation, CreatableRelationProvider}
import za.co.absa.spline.harvester.plugin.Plugin._

/**
 * Resolves a data source provider name or a data format name. E.g. Mongo, Hive, Avro, XML, Parquet etc.
 * <p>The input value depends on the Spark version in use and the logical plan node being processed.
 * <p> Usually it's `String`, [[org.apache.spark.sql.sources.RelationProvider]] or
 * [[org.apache.spark.sql.execution.datasources.FileFormat]]
 */
trait DataSourceFormatNameResolving {
  self: Plugin =>
  def formatNameResolver: PartialFunction[AnyRef, String]
}

/**
 * Matches on logical plan [[org.apache.spark.sql.catalyst.plans.logical.LeafNode]]
 * and [[org.apache.spark.sql.catalyst.plans.logical.Command]] nodes,
 * capturing ones that represent `read`-operations.
 */
trait ReadNodeProcessing {
  self: Plugin =>
  def readNodeProcessor: PartialFunction[LogicalPlan, ReadNodeInfo]
}

/**
 * Matches on the logical plan root node (usually [[org.apache.spark.sql.catalyst.plans.logical.Command]]),
 * capturing `write`-operations.
 */
trait WriteNodeProcessing {
  self: Plugin =>
  def writeNodeProcessor: PartialFunction[LogicalPlan, WriteNodeInfo]
}

/**
 * Similar to [[ReadNodeProcessing]], but narrowed down to specifically capturing [[LogicalRelation]] nodes.
 * <p> It matches on a [[BaseRelation]] object that in most cases contain all information about a read-operation.
 * <p> This is the most often use-case. Usually this is how `read`-operations are represented in Spark logical plans.
 *
 * @see [[za.co.absa.spline.harvester.plugin.composite.LogicalRelationPlugin]]
 * @see [[RelationProviderProcessing]]
 */
trait BaseRelationProcessing {
  self: Plugin =>
  def baseRelationProcessor: PartialFunction[(BaseRelation, LogicalRelation), ReadNodeInfo]
}

/**
 * Similar to [[WriteNodeProcessing]], but is tailored for capturing
 * [[org.apache.spark.sql.execution.datasources.SaveIntoDataSourceCommand]] nodes.
 * <p> It matches on a `relation provider` object that might be
 * a `String` or a [[CreatableRelationProvider]] (depends on the Spark version in use).
 * <p> This is the most often use-case. Usually this is how `write`-operations are represented in Spark logical plans.
 *
 * @see [[za.co.absa.spline.harvester.plugin.composite.SaveIntoDataSourceCommandPlugin]]
 * @see [[BaseRelationProcessing]]
 */
trait RelationProviderProcessing {
  self: Plugin =>
  def relationProviderProcessor: PartialFunction[(AnyRef, SaveIntoDataSourceCommand), WriteNodeInfo]
}
