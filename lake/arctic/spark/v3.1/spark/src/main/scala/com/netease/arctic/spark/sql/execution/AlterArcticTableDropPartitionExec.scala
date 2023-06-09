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

import com.netease.arctic.op.OverwriteBaseFiles
import com.netease.arctic.spark.table.{ArcticIcebergSparkTable, ArcticSparkTable}
import org.apache.iceberg.spark.SparkFilters
import org.apache.spark.sql.arctic.catalyst.ExpressionHelper
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.analysis.{PartitionSpec, UnresolvedPartitionSpec}
import org.apache.spark.sql.catalyst.expressions.{And, Attribute, AttributeReference, EqualNullSafe, Expression, Literal}
import org.apache.spark.sql.connector.catalog.Table
import org.apache.spark.sql.execution.datasources.v2.V2CommandExec
import org.apache.spark.sql.types._

import java.util
import scala.collection.JavaConverters.asJavaIterableConverter

case class AlterArcticTableDropPartitionExec(
  table: Table,
  parts: Seq[PartitionSpec],
  retainData: Boolean
) extends V2CommandExec {
  override protected def run(): Seq[InternalRow] = {
    // build partitions
    val specs = parts.map {
      case part: UnresolvedPartitionSpec =>
        part.spec.map(s => s._1 + "=" + s._2).asJava
    }
    val ident = specs.mkString.replace(", ", "/")
      .replace("[", "")
      .replace("]", "")
    val partitions = ident.split("/")
    // build expressions
    var i = 0
    var deleteExpr: Expression = null
    val expressions = new util.ArrayList[Expression]
    while (i < partitions.size) {
      val partitionData = partitions.apply(i).split("=")

      val dataType = table.schema().apply(partitionData.apply(0)).dataType
      val data = convertDataByType(dataType, partitionData.apply(1))
      val experssion = EqualNullSafe(
        AttributeReference(
          partitionData.apply(0),
          dataType)(),
        Literal(data))
      expressions.add(experssion)
      i += 1
    }
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
          val overwriteBaseFiles: OverwriteBaseFiles = arctic.table().asKeyedTable().newOverwriteBaseFiles()
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

  def convertDataByType(dataType: DataType, data: String): Any = {
    dataType match {
      case BinaryType => data.toByte
      case IntegerType => data.toInt
      case BooleanType => data.toBoolean
      case LongType => data.toLong
      case DoubleType => data.toDouble
      case FloatType => data.toFloat
      case ShortType => data.toShort
      case ByteType => data.toByte
      case StringType => data
      case _ =>
        throw new UnsupportedOperationException("Cannot convert data by this type")
    }
  }

  override def output: Seq[Attribute] = Nil

}
