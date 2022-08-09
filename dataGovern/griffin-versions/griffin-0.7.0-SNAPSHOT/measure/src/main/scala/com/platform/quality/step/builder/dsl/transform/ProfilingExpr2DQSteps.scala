/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.platform.quality.step.builder.dsl.transform

import com.platform.quality.configuration.dqdefinition.RuleParam
import com.platform.quality.step.builder.ConstantColumns
import com.platform.quality.step.builder.dsl.expr.{AliasableExpr, Expr, FromClause, GroupbyClause, LiteralStringExpr, ProfilingClause}
import com.platform.quality.step.builder.dsl.transform.analyzer.ProfilingAnalyzer
import org.apache.commons.lang.StringUtils
import com.platform.quality.configuration.enums.FlattenType.DefaultFlattenType
import com.platform.quality.configuration.enums.OutputType._
import com.platform.quality.configuration.enums.ProcessType._
import com.platform.quality.context.DQContext
import com.platform.quality.step.{DQStep, write}
import com.platform.quality.step.transform.SparkSqlTransformStep
import com.platform.quality.step.write.MetricWriteStep
import org.apache.griffin.measure.step.builder.dsl.expr._
import com.platform.quality.utils.ParamUtil._

/**
 * generate profiling dq steps
 */
case class ProfilingExpr2DQSteps(context: DQContext, expr: Expr, ruleParam: RuleParam)
    extends Expr2DQSteps {

  private object ProfilingKeys {
    val _source = "source"
  }
  import ProfilingKeys._

  def getDQSteps: Seq[DQStep] = {
    val details = ruleParam.getDetails
    val profilingExpr = expr.asInstanceOf[ProfilingClause]

    val sourceName = profilingExpr.fromClauseOpt match {
      case Some(fc) => fc.dataSource
      case _ => details.getString(_source, context.getDataSourceName(0))
    }
    val fromClause = profilingExpr.fromClauseOpt.getOrElse(FromClause(sourceName)).desc

    val procType = context.procType
    val timestamp = context.contextId.timestamp

    if (!context.runTimeTableRegister.existsTable(sourceName)) {
      warn(s"[$timestamp] data source $sourceName not exists")
      Nil
    } else {
      val analyzer = analyzer.ProfilingAnalyzer(profilingExpr, sourceName)
      val selExprDescs = analyzer.selectionExprs.map { sel =>
        val alias = sel match {
          case s: AliasableExpr =>
            s.alias.filter(StringUtils.isNotEmpty).map(a => s" AS `$a`").getOrElse("")

          case _ => ""
        }
        s"${sel.desc}$alias"
      }
      val selCondition = profilingExpr.selectClause.extraConditionOpt.map(_.desc).mkString
      val selClause = procType match {
        case BatchProcessType => selExprDescs.mkString(", ")
        case StreamingProcessType => (s"`${ConstantColumns.tmst}`" +: selExprDescs).mkString(", ")
      }
      val groupByClauseOpt = analyzer.groupbyExprOpt
      val groupbyClause = procType match {
        case BatchProcessType => groupByClauseOpt.map(_.desc).getOrElse("")
        case StreamingProcessType =>
          val tmstGroupbyClause =
            GroupbyClause(LiteralStringExpr(s"`${ConstantColumns.tmst}`") :: Nil, None)
          val mergedGroubbyClause = tmstGroupbyClause.merge(groupByClauseOpt match {
            case Some(gbc) => gbc
            case _ => GroupbyClause(Nil, None)
          })
          mergedGroubbyClause.desc
      }
      val preGroupbyClause = analyzer.preGroupbyExprs.map(_.desc).mkString(" ")
      val postGroupbyClause = analyzer.postGroupbyExprs.map(_.desc).mkString(" ")

      // 1. select statement
      val profilingSql = {
        s"SELECT $selCondition $selClause " +
          s"$fromClause $preGroupbyClause $groupbyClause $postGroupbyClause"
      }
      val profilingName = ruleParam.getOutDfName()
      val profilingMetricWriteStep = {
        val metricOpt = ruleParam.getOutputOpt(MetricOutputType)
        val mwName = metricOpt.flatMap(_.getNameOpt).getOrElse(ruleParam.getOutDfName())
        val flattenType = metricOpt.map(_.getFlatten).getOrElse(DefaultFlattenType)
        write.MetricWriteStep(mwName, profilingName, flattenType)
      }
      val profilingTransStep =
        SparkSqlTransformStep(
          profilingName,
          profilingSql,
          details,
          Some(profilingMetricWriteStep))
      profilingTransStep :: Nil
    }
  }

}
