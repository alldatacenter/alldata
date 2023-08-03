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

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
import scala.collection.mutable.ArrayBuffer

import com.netease.arctic.spark.table.ArcticSparkTable
import com.netease.arctic.table.KeyedTable
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.catalyst.expressions.{Attribute, AttributeReference}
import org.apache.spark.sql.connector.catalog._
import org.apache.spark.sql.execution.datasources.v2.LeafV2CommandExec
import org.apache.spark.sql.types.{MetadataBuilder, StringType, StructField, StructType}

case class DescribeKeyedTableExec(
    table: Table,
    catalog: TableCatalog,
    ident: Identifier,
    isExtended: Boolean) extends LeafV2CommandExec {
  val outputAttrs: Seq[AttributeReference] = Seq(
    AttributeReference(
      "col_name",
      StringType,
      nullable = false,
      new MetadataBuilder().putString("comment", "name of the column").build())(),
    AttributeReference(
      "data_type",
      StringType,
      nullable = false,
      new MetadataBuilder().putString("comment", "data type of the column").build())(),
    AttributeReference(
      "comment",
      StringType,
      nullable = true,
      new MetadataBuilder().putString("comment", "comment of the column").build())())

  private[sql] def fromAttributes(attributes: Seq[Attribute]): StructType =
    StructType(attributes.map(a => StructField(a.name, a.dataType, a.nullable, a.metadata)))

  private val toRow = {
    RowEncoder(fromAttributes(outputAttrs)).resolveAndBind().createSerializer()
  }

  private def addPrimaryColumns(
      rows: ArrayBuffer[InternalRow],
      keyedTable: ArcticSparkTable): Unit = {
    keyedTable.table() match {
      case table: KeyedTable =>
        rows += emptyRow()
        rows += toCatalystRow("# Primary keys", "", "")
        if (!table.primaryKeySpec.primaryKeyExisted()) {
          rows += toCatalystRow("Not keyed table", "", "")
        } else {
          table.primaryKeySpec().primaryKeyStruct().fields().toSeq.foreach(k => {
            rows += toCatalystRow(k.name(), k.`type`().toString, "")
          })
        }
      case _ =>
    }
  }

  override protected def run(): Seq[InternalRow] = {
    val rows = new ArrayBuffer[InternalRow]()
    addSchema(rows, table)
    addPartitioning(rows, table)

    if (isExtended) {
      addMetadataColumns(rows, table)
      addTableDetails(rows, table)
    }

    table match {
      case keyedTable: ArcticSparkTable =>
        addPrimaryColumns(rows, keyedTable)
      case _ =>
        Nil
    }
    rows.toSeq
  }

  val TABLE_RESERVED_PROPERTIES =
    Seq(
      TableCatalog.PROP_COMMENT,
      TableCatalog.PROP_LOCATION,
      TableCatalog.PROP_PROVIDER,
      TableCatalog.PROP_OWNER)

  private def addTableDetails(rows: ArrayBuffer[InternalRow], table: Table): Unit = {
    rows += emptyRow()
    rows += toCatalystRow("# Detailed Table Information", "", "")
    rows += toCatalystRow("Name", table.name(), "")

    TABLE_RESERVED_PROPERTIES.foreach(propKey => {
      if (table.properties.containsKey(propKey)) {
        rows += toCatalystRow(propKey.capitalize, table.properties.get(propKey), "")
      }
    })
    val properties =
      table.properties.asScala.toList
        .filter(kv => !TABLE_RESERVED_PROPERTIES.contains(kv._1))
        .sortBy(_._1).map {
          case (key, value) => key + "=" + value
        }.mkString("[", ",", "]")
    rows += toCatalystRow("Table Properties", properties, "")
  }

  private def addSchema(rows: ArrayBuffer[InternalRow], table: Table): Unit = {
    rows ++= table.schema.map { column =>
      toCatalystRow(
        column.name,
        column.dataType.simpleString,
        column.getComment().getOrElse(""))
    }
  }

  private def addMetadataColumns(rows: ArrayBuffer[InternalRow], table: Table): Unit = table match {
    case hasMeta: SupportsMetadataColumns if hasMeta.metadataColumns.nonEmpty =>
      rows += emptyRow()
      rows += toCatalystRow("# Metadata Columns", "", "")
      rows ++= hasMeta.metadataColumns.map { column =>
        toCatalystRow(
          column.name,
          column.dataType.simpleString,
          Option(column.comment()).getOrElse(""))
      }
    case _ =>
  }

  private def addPartitioning(rows: ArrayBuffer[InternalRow], table: Table): Unit = {
    rows += emptyRow()
    rows += toCatalystRow("# Partitioning", "", "")
    if (table.partitioning.isEmpty) {
      rows += toCatalystRow("Not partitioned", "", "")
    } else {
      rows ++= table.partitioning.zipWithIndex.map {
        case (transform, index) => toCatalystRow(s"Part $index", transform.describe(), "")
      }
    }
  }

  private def emptyRow(): InternalRow = toCatalystRow("", "", "")

  override def output: Seq[Attribute] = outputAttrs
}
