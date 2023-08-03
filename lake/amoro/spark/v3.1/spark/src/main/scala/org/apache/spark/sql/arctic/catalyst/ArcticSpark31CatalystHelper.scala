/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.arctic.catalyst

import com.netease.arctic.data.PrimaryKeyData
import com.netease.arctic.spark.SparkInternalRowWrapper
import com.netease.arctic.spark.sql.connector.expressions.FileIndexBucket
import org.apache.iceberg.Schema
import org.apache.iceberg.spark.SparkSchemaUtil
import org.apache.spark.sql.{catalyst, connector, AnalysisException}
import org.apache.spark.sql.catalyst.{InternalRow, SQLConfHelper}
import org.apache.spark.sql.catalyst.expressions.{Expression, IcebergBucketTransform, IcebergDayTransform, IcebergHourTransform, IcebergMonthTransform, IcebergTruncateTransform, IcebergYearTransform, NamedExpression, NullIntolerant}
import org.apache.spark.sql.catalyst.expressions.codegen.CodegenFallback
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.connector.catalog.CatalogV2Implicits
import org.apache.spark.sql.connector.expressions._
import org.apache.spark.sql.connector.iceberg.expressions.{NullOrdering, SortDirection, SortOrder}
import org.apache.spark.sql.types.{DataType, IntegerType, LongType}

object ArcticSpark31CatalystHelper extends SQLConfHelper {

  def toCatalyst(expr: connector.expressions.Expression, query: LogicalPlan): Expression = {
    def resolve(parts: Seq[String]): NamedExpression = {
      val resolver = conf.resolver
      query.resolve(parts, resolver) match {
        case Some(attr) =>
          attr
        case None =>
          val ref = parts.map(CatalogV2Implicits.quoteIfNeeded).mkString(".")
          throw new AnalysisException(s"Cannot resolve '$ref' using ${query.output}")
      }
    }

    expr match {
      case it: IdentityTransform =>
        resolve(it.ref.fieldNames)
      case fbt: FileIndexBucket =>
        ArcticFileIndexBucketTransform(fbt.mask(), fbt.primaryKeyData(), fbt.schema())
      case ArcticBucketTransform(n, r) =>
        IcebergBucketTransform(n, ExpressionHelper.resolveRef[NamedExpression](r, query))
      case yt: YearsTransform =>
        IcebergYearTransform(resolve(yt.ref.fieldNames))
      case mt: MonthsTransform =>
        IcebergMonthTransform(resolve(mt.ref.fieldNames))
      case dt: DaysTransform =>
        IcebergDayTransform(resolve(dt.ref.fieldNames))
      case ht: HoursTransform =>
        IcebergHourTransform(resolve(ht.ref.fieldNames))
      case ref: FieldReference =>
        resolve(ref.fieldNames)
      case TruncateTransform(n, ref) =>
        IcebergTruncateTransform(resolve(ref.fieldNames), width = n)
      case sort: SortOrder =>
        val catalystChild = toCatalyst(sort.expression(), query)
        catalyst.expressions.SortOrder(
          catalystChild,
          toCatalystSortDirection(sort.direction()),
          toCatalystNullOrdering(sort.nullOrdering()),
          Seq.empty)
      case _ =>
        throw new RuntimeException(s"$expr is not currently supported")
    }
  }

  private def toCatalystSortDirection(direction: SortDirection)
      : catalyst.expressions.SortDirection = {
    direction match {
      case SortDirection.ASCENDING => catalyst.expressions.Ascending
      case SortDirection.DESCENDING => catalyst.expressions.Descending
    }
  }

  private def toCatalystNullOrdering(nullOrdering: NullOrdering)
      : catalyst.expressions.NullOrdering = {
    nullOrdering match {
      case NullOrdering.NULLS_FIRST => catalyst.expressions.NullsFirst
      case NullOrdering.NULLS_LAST => catalyst.expressions.NullsLast
    }
  }

  private object ArcticBucketTransform {
    def unapply(transform: Transform): Option[(Int, FieldReference)] = transform match {
      case bt: BucketTransform => bt.columns match {
          case Seq(nf: NamedReference) =>
            Some(bt.numBuckets.value(), FieldReference(nf.fieldNames()))
          case _ =>
            None
        }
      case _ => None
    }
  }

  private object Lit {
    def unapply[T](literal: Literal[T]): Some[(T, DataType)] = {
      Some((literal.value(), literal.dataType()))
    }
  }

  private object TruncateTransform {
    def unapply(transform: Transform): Option[(Int, FieldReference)] = transform match {
      case ApplyTransform(name, args) if name.equalsIgnoreCase("truncate") =>
        args match {
          case Seq(ref: NamedReference, Lit(value: Int, IntegerType)) =>
            Some(value, FieldReference(ref.fieldNames()))
          case Seq(Lit(value: Int, IntegerType), ref: NamedReference) =>
            Some(value, FieldReference(ref.fieldNames()))
          case _ => None
        }
      case _ => None
    }
  }

  case class ArcticFileIndexBucketTransform(
      numBuckets: Int,
      keyData: PrimaryKeyData,
      schema: Schema)
    extends Expression with CodegenFallback with NullIntolerant {

    @transient
    lazy val internalRowToStruct: SparkInternalRowWrapper =
      new SparkInternalRowWrapper(SparkSchemaUtil.convert(schema))

    override def dataType: DataType = LongType

    override def eval(input: InternalRow): Any = {
      if (null == input) {
        return null
      }
      internalRowToStruct.wrap(input)
      keyData.primaryKey(internalRowToStruct)
      val node = keyData.treeNode(numBuckets)
      node.getIndex
    }

    override def nullable: Boolean = true

    override def children: Seq[Expression] = Nil

    override def toString(): String = {
      s"ArcticFileIndexBucket($numBuckets)"
    }
  }
}
