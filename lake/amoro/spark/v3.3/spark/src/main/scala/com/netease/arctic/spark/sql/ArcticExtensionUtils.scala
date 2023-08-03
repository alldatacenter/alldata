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

package com.netease.arctic.spark.sql

import scala.collection.JavaConverters.seqAsJavaList

import com.netease.arctic.spark.{ArcticSparkCatalog, ArcticSparkSessionCatalog}
import com.netease.arctic.spark.table.{ArcticIcebergSparkTable, ArcticSparkTable, SupportsRowLevelOperator}
import org.apache.iceberg.spark.Spark3Util
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.TableIdentifier
import org.apache.spark.sql.catalyst.analysis.Resolver
import org.apache.spark.sql.catalyst.plans.logical.{LogicalPlan, Project, SubqueryAlias}
import org.apache.spark.sql.connector.catalog.{CatalogPlugin, Identifier, Table, TableCatalog}
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Relation
import org.apache.spark.sql.types.{ArrayType, MapType, StructField, StructType}

object ArcticExtensionUtils {

  implicit class ArcticTableHelper(table: Table) {
    def asArcticTable: ArcticSparkTable = {
      table match {
        case arcticTable: ArcticSparkTable => arcticTable
        case _ => throw new IllegalArgumentException(s"$table is not an arctic table")
      }
    }

    def asUpsertWrite: SupportsRowLevelOperator = {
      table match {
        case arcticTable: SupportsRowLevelOperator => arcticTable
        case _ => throw new IllegalArgumentException(s"$table is not an upsert-able table")
      }
    }
  }

  implicit class ArcticRelationHelper(plan: LogicalPlan) {
    def asTableRelation: DataSourceV2Relation = {
      ArcticExtensionUtils.asTableRelation(plan)
    }
  }

  def isArcticRelation(plan: LogicalPlan): Boolean = {
    def isArcticTable(relation: DataSourceV2Relation): Boolean = relation.table match {
      case _: ArcticSparkTable => true
      case _ => false
    }

    plan.collectLeaves().exists {
      case p: DataSourceV2Relation => isArcticTable(p)
      case s: SubqueryAlias => s.child.children.exists { case p: DataSourceV2Relation =>
          isArcticTable(p)
        }
      case _ => false
    }
  }

  def isArcticKeyedRelation(plan: LogicalPlan): Boolean = {
    def isArcticKeyedTable(relation: DataSourceV2Relation): Boolean = relation.table match {
      case a: ArcticSparkTable =>
        a.table().isKeyedTable
      case _ => false
    }

    plan.collectLeaves().exists {
      case p: DataSourceV2Relation => isArcticKeyedTable(p)
      case s: SubqueryAlias => s.child.children.exists { case p: DataSourceV2Relation =>
        isArcticKeyedTable(p)
      }
      case _ => false
    }
  }

  def isUpsert(relation: DataSourceV2Relation): Boolean = {
    val upsertWrite = relation.table.asUpsertWrite
    upsertWrite.appendAsUpsert()
  }

  def isArcticIcebergRelation(plan: LogicalPlan): Boolean = {
    def isArcticIcebergTable(relation: DataSourceV2Relation): Boolean = relation.table match {
      case _: ArcticIcebergSparkTable => true
      case _ => false
    }

    plan.collectLeaves().exists {
      case p: DataSourceV2Relation => isArcticIcebergTable(p)
      case s: SubqueryAlias => s.child.children.exists {
          case p: DataSourceV2Relation => isArcticIcebergTable(p)
        }
    }
  }

  def isArcticCatalog(catalog: TableCatalog): Boolean = {
    catalog match {
      case _: ArcticSparkCatalog => true
      case _: ArcticSparkSessionCatalog[_] => true
      case _ => false
    }
  }

  def isArcticCatalog(catalog: CatalogPlugin): Boolean = {
    catalog match {
      case _: ArcticSparkCatalog => true
      case _: ArcticSparkSessionCatalog[_] => true
      case _ => false
    }
  }

  def isArcticTable(table: Table): Boolean = table match {
    case _: ArcticSparkTable => true
    case _: ArcticIcebergSparkTable => true
    case _ => false
  }

  def asTableRelation(plan: LogicalPlan): DataSourceV2Relation = {
    plan match {
      case s: SubqueryAlias => asTableRelation(s.child)
      case p: Project => asTableRelation(p.child.children.head)
      case r: DataSourceV2Relation => r
      case _ => throw new IllegalArgumentException("Expected a DataSourceV2Relation")
    }
  }

  def isKeyedTable(relation: DataSourceV2Relation): Boolean = {
    relation.table match {
      case arctic: ArcticSparkTable =>
        arctic.table().isKeyedTable
      case _ => false
    }
  }

  def buildCatalogAndIdentifier(
      sparkSession: SparkSession,
      originIdentifier: TableIdentifier): (TableCatalog, Identifier) = {
    var identifier: Seq[String] = Seq.empty[String]
    identifier :+= originIdentifier.database.get
    identifier :+= originIdentifier.table
    val catalogAndIdentifier =
      Spark3Util.catalogAndIdentifier(sparkSession, seqAsJavaList(identifier))
    catalogAndIdentifier.catalog() match {
      case a: TableCatalog => (a, catalogAndIdentifier.identifier())
      case _ =>
        throw new UnsupportedOperationException("Only support TableCatalog or its implementation")
    }
  }
}
