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

import com.netease.arctic.spark.table.{ArcticIcebergSparkTable, ArcticSparkTable, SupportsUpsert}
import com.netease.arctic.spark.{ArcticSparkCatalog, ArcticSparkSessionCatalog}
import org.apache.iceberg.spark.Spark3Util
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.TableIdentifier
import org.apache.spark.sql.catalyst.analysis.Resolver
import org.apache.spark.sql.catalyst.plans.logical.{LogicalPlan, Project, SubqueryAlias}
import org.apache.spark.sql.connector.catalog.{Identifier, Table, TableCatalog}
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Relation
import org.apache.spark.sql.types.{ArrayType, MapType, StructField, StructType}

import scala.collection.JavaConverters.seqAsJavaList


object ArcticExtensionUtils {

  implicit class ArcticTableHelper(table: Table) {
    def asArcticTable: ArcticSparkTable = {
      table match {
        case arcticTable: ArcticSparkTable => arcticTable
        case _ => throw new IllegalArgumentException(s"$table is not an arctic table")
      }
    }

    def asUpsertWrite: SupportsUpsert = {
      table match {
        case arcticTable: SupportsUpsert => arcticTable
        case _ => throw new IllegalArgumentException(s"$table is not an upsert-able table")
      }
    }
  }

  implicit class ArcticRelationHelper(plan: LogicalPlan) {
    def asTableRelation: DataSourceV2Relation = {
      ArcticExtensionUtils.asTableRelation(plan)
    }
  }

  implicit class ArcticStructTypeHelper(struct: StructType) {
    def findNestedField(fieldNames: Seq[String],
                        includeCollections: Boolean = false,
                        resolver: Resolver = _ == _): Option[(Seq[String], StructField)] = {
      def prettyFieldName(nameParts: Seq[String]): String = {
        nameParts.map(quoteIfNeeded).mkString(".")
      }

      def quoteIfNeeded(part: String): String = {
        if (part.contains(".") || part.contains("`")) {
          s"`${part.replace("`", "``")}`"
        } else {
          part
        }
      }


      def findField(
                     struct: StructType,
                     searchPath: Seq[String],
                     normalizedPath: Seq[String]): Option[(Seq[String], StructField)] = {
        searchPath.headOption.flatMap { searchName =>
          val found = struct.fields.filter(f => resolver(searchName, f.name))
          if (found.length > 1) {
            val names = found.map(f => prettyFieldName(normalizedPath :+ f.name))
              .mkString("[", ", ", " ]")
            throw new UnsupportedOperationException(
              s"Ambiguous field name: ${prettyFieldName(normalizedPath :+ searchName)}. Found " +
                s"multiple columns that can match: $names")
          } else if (found.isEmpty) {
            None
          } else {
            val field = found.head
            (searchPath.tail, field.dataType, includeCollections) match {
              case (Seq(), _, _) =>
                Some(normalizedPath -> field)

              case (names, struct: StructType, _) =>
                findField(struct, names, normalizedPath :+ field.name)

              case (_, _, false) =>
                None // types nested in maps and arrays are not used

              case (Seq("key"), MapType(keyType, _, _), true) =>
                // return the key type as a struct field to include nullability
                Some((normalizedPath :+ field.name) -> StructField("key", keyType, nullable = false))

              case (Seq("key", names@_*), MapType(struct: StructType, _, _), true) =>
                findField(struct, names, normalizedPath ++ Seq(field.name, "key"))

              case (Seq("value"), MapType(_, valueType, isNullable), true) =>
                // return the value type as a struct field to include nullability
                Some((normalizedPath :+ field.name) ->
                  StructField("value", valueType, nullable = isNullable))

              case (Seq("value", names@_*), MapType(_, struct: StructType, _), true) =>
                findField(struct, names, normalizedPath ++ Seq(field.name, "value"))

              case (Seq("element"), ArrayType(elementType, isNullable), true) =>
                // return the element type as a struct field to include nullability
                Some((normalizedPath :+ field.name) ->
                  StructField("element", elementType, nullable = isNullable))

              case (Seq("element", names@_*), ArrayType(struct: StructType, _), true) =>
                findField(struct, names, normalizedPath ++ Seq(field.name, "element"))

              case _ =>
                None
            }
          }
        }
      }

      findField(struct, fieldNames, Nil)
    }
  }

  def isArcticRelation(plan: LogicalPlan): Boolean = {
    def isArcticTable(relation: DataSourceV2Relation): Boolean = relation.table match {
      case _: ArcticSparkTable => true
      case _ => false
    }

    plan.collectLeaves().exists {
      case p: DataSourceV2Relation => isArcticTable(p)
      case s: SubqueryAlias => s.child.children.exists { case p: DataSourceV2Relation => isArcticTable(p) }
      case _ => false
    }
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

  def buildCatalogAndIdentifier(sparkSession: SparkSession, originIdentifier: TableIdentifier): (TableCatalog, Identifier) = {
    var identifier: Seq[String] = Seq.empty[String]
    identifier :+= originIdentifier.database.get
    identifier :+= originIdentifier.table
    val catalogAndIdentifier = Spark3Util.catalogAndIdentifier(sparkSession, seqAsJavaList(identifier))
    catalogAndIdentifier.catalog() match {
      case a: TableCatalog => (a, catalogAndIdentifier.identifier())
      case _ => throw new UnsupportedOperationException("Only support TableCatalog or its implementation")
    }
  }
}
