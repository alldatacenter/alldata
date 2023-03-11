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

package com.netease.arctic.spark.sql.catalyst.rule

import com.netease.arctic.spark.distibutions.{BucketTransform, FieldReference, IdentityTransform}
import com.netease.arctic.spark.distributions.{ClusteredDistribution, NamedReference, Transform, Expression => V2Expression}
import com.netease.arctic.spark.source.ArcticSparkTable
import com.netease.arctic.spark.sql.catalyst.expressions.BucketExpression
import com.netease.arctic.spark.sql.plan.OverwriteArcticTableDynamic
import com.netease.arctic.spark.sql.util.ImplicitHelper._
import com.netease.arctic.spark.util.ArcticSparkUtil
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.arctic.AnalysisException
import org.apache.spark.sql.catalyst.expressions.{Expression, NamedExpression}
import org.apache.spark.sql.catalyst.optimizer.PropagateEmptyRelation.conf
import org.apache.spark.sql.catalyst.plans.logical.{LogicalPlan, RepartitionByExpression}
import org.apache.spark.sql.catalyst.rules.Rule

case class OptimizeWriteRule(spark: SparkSession) extends Rule[LogicalPlan] {

  override def apply(plan: LogicalPlan): LogicalPlan = plan transformDown {
    case a @ OverwriteArcticTableDynamic(_, table, query) =>
      table match {
        case table: ArcticSparkTable =>
          val newQuery = distributionQuery(query, table)
          val optimizedOverwriteTable = a.copy(query = newQuery)
          optimizedOverwriteTable
        case _ =>
          a
      }
  }

  private def distributionQuery(query: LogicalPlan, table: ArcticSparkTable): LogicalPlan = {
    val distribution = ArcticSparkUtil.buildRequiredDistribution(table) match {
      case d: ClusteredDistribution =>
        d.clustering.map(e => toCatalyst(e, query))
      case _ =>
        Array.empty[Expression]
    }
    val queryWithDistribution = if (distribution.nonEmpty) {
      val partitionNum = conf.numShufflePartitions
      val pp = RepartitionByExpression(distribution, query, partitionNum)
      pp
    } else {
      query
    }
    queryWithDistribution
  }

  def toCatalyst(expr: V2Expression, query: LogicalPlan): Expression = {
    val resolver = conf.resolver
    def resolve(parts: Seq[String]): NamedExpression = {
      query.resolve(parts, resolver) match {
        case Some(attr) =>
          attr
        case None =>
          val ref = parts.map(quoteIfNeeded).mkString(".")
          throw AnalysisException.message(s"Cannot resolve '$ref' using ${query.output}")
      }
    }

    expr match {
      case it: IdentityTransform =>
        resolve(it.ref.fieldNames)
      case ArcticBucketTransform(n, r) =>
        BucketExpression(n, resolveRef[NamedExpression](r, query))
      case ref: FieldReference =>
        resolve(ref.fieldNames)
      case _ =>
        throw new RuntimeException(s"$expr is not currently supported")
    }
  }


  def resolveRef[T <: NamedExpression](ref: NamedReference, plan: LogicalPlan): T = {
    plan.resolve(ref.fieldNames.toSeq, conf.resolver) match {
      case Some(namedExpr) =>
        namedExpr.asInstanceOf[T]
      case None =>
        val name = ref.fieldNames.toSeq.quoted
        throw AnalysisException.message(s"Cannot resolve '$ref' using ${name}")
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
}
