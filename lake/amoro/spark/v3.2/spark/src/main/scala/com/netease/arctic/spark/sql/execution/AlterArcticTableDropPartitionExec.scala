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

import java.util

import com.netease.arctic.op.OverwriteBaseFiles
import com.netease.arctic.spark.table.{ArcticIcebergSparkTable, ArcticSparkTable}
import org.apache.iceberg.spark.SparkFilters
import org.apache.spark.sql.arctic.catalyst.ExpressionHelper
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.analysis.{PartitionSpec, ResolvedPartitionSpec}
import org.apache.spark.sql.catalyst.expressions.{And, Attribute, AttributeReference, EqualNullSafe, Expression, Literal}
import org.apache.spark.sql.connector.catalog.Table
import org.apache.spark.sql.execution.datasources.v2.LeafV2CommandExec
import org.apache.spark.sql.types._

case class AlterArcticTableDropPartitionExec(
    table: Table,
    parts: Seq[PartitionSpec]) extends LeafV2CommandExec {
  override protected def run(): Seq[InternalRow] = {
    // build partitions
    val rows: Seq[InternalRow] = parts.map {
      case ResolvedPartitionSpec(_, ident, _) =>
        ident
    }
    val names: Seq[Seq[String]] = parts.map {
      case ResolvedPartitionSpec(names, _, _) =>
        names
    }
    val expressions = new util.ArrayList[Expression]
    var index = 0;
    while (index < names.size) {
      var i = 0
      val name = names.apply(index)
      val row = rows.apply(index)
      while (i < name.size) {
        val dataType = table.schema().apply(name.apply(i)).dataType
        val data = convertInternalRowDataByType(dataType, row, i)
        val experssion = EqualNullSafe(
          AttributeReference(
            name.apply(i),
            dataType)(),
          Literal(data))
        expressions.add(experssion)
        i += 1
      }
      index += 1
    }

    var deleteExpr: Expression = null
    expressions.forEach(exp => {
      if (deleteExpr == null) {
        deleteExpr = exp
      } else {
        deleteExpr = And(deleteExpr, exp)
      }
    });
    // build filters
    val filters = splitConjunctivePredicates(deleteExpr).map {
      filter =>
        ExpressionHelper.translateFilter(deleteExpr).getOrElse(
          throw new UnsupportedOperationException("Cannot translate expression to source filter"))
    }.toArray
    val expression = SparkFilters.convert(filters)
    table match {
      case arctic: ArcticSparkTable =>
        if (arctic.table().isKeyedTable) {
          val txId = arctic.table().asKeyedTable().beginTransaction(null)
          val overwriteBaseFiles: OverwriteBaseFiles =
            arctic.table().asKeyedTable().newOverwriteBaseFiles()
          overwriteBaseFiles.overwriteByRowFilter(expression)
          overwriteBaseFiles.updateOptimizedSequenceDynamically(txId)
          overwriteBaseFiles.commit()
        } else {
          val overwriteFiles = arctic.table().asUnkeyedTable().newOverwrite()
          overwriteFiles.overwriteByRowFilter(expression)
          overwriteFiles.commit()
        }
      case arctic: ArcticIcebergSparkTable =>
        val overwriteFiles = arctic.table().newOverwrite()
        overwriteFiles.overwriteByRowFilter(expression)
        overwriteFiles.commit()
    }
    Nil
  }

  def splitConjunctivePredicates(condition: Expression): Seq[Expression] = {
    condition match {
      case And(cond1, cond2) =>
        splitConjunctivePredicates(cond1) ++ splitConjunctivePredicates(cond2)
      case other => other :: Nil
    }
  }

  def convertInternalRowDataByType(dataType: DataType, data: InternalRow, index: Int): Any = {
    dataType match {
      case BinaryType => data.getBinary(index)
      case IntegerType => data.getInt(index)
      case BooleanType => data.getBoolean(index)
      case LongType => data.getLong(index)
      case DoubleType => data.getDouble(index)
      case FloatType => data.getFloat(index)
      case ShortType => data.getShort(index)
      case ByteType => data.getByte(index)
      case StringType => data.getString(index)
      case _ =>
        throw new UnsupportedOperationException("Cannot convert data by this type")
    }
  }

  override def output: Seq[Attribute] = Nil
}
