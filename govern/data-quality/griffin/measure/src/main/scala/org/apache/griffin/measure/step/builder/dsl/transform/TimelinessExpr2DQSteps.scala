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

package org.apache.griffin.measure.step.builder.dsl.transform

import org.apache.griffin.measure.configuration.dqdefinition.RuleParam
import org.apache.griffin.measure.configuration.enums.FlattenType.{
  ArrayFlattenType,
  DefaultFlattenType
}
import org.apache.griffin.measure.configuration.enums.OutputType._
import org.apache.griffin.measure.configuration.enums.ProcessType._
import org.apache.griffin.measure.context.DQContext
import org.apache.griffin.measure.step.DQStep
import org.apache.griffin.measure.step.builder.ConstantColumns
import org.apache.griffin.measure.step.builder.dsl.expr._
import org.apache.griffin.measure.step.builder.dsl.transform.analyzer.TimelinessAnalyzer
import org.apache.griffin.measure.step.transform.SparkSqlTransformStep
import org.apache.griffin.measure.step.write.{MetricWriteStep, RecordWriteStep}
import org.apache.griffin.measure.utils.ParamUtil._
import org.apache.griffin.measure.utils.TimeUtil

/**
 * generate timeliness dq steps
 */
case class TimelinessExpr2DQSteps(context: DQContext, expr: Expr, ruleParam: RuleParam)
    extends Expr2DQSteps {

  private object TimelinessKeys {
    val _source = "source"
    val _latency = "latency"
    val _total = "total"
    val _avg = "avg"
    val _threshold = "threshold"
    val _step = "step"
    val _count = "count"
    val _stepSize = "step.size"
    val _percentileColPrefix = "percentile"
    val _percentileValues = "percentile.values"
  }
  import TimelinessKeys._

  def getDQSteps: Seq[DQStep] = {
    val details = ruleParam.getDetails
    val timelinessExpr = expr.asInstanceOf[TimelinessClause]

    val sourceName = details.getString(_source, context.getDataSourceName(0))

    val procType = context.procType
    val timestamp = context.contextId.timestamp

    if (!context.runTimeTableRegister.existsTable(sourceName)) {
      warn(s"[$timestamp] data source $sourceName not exists")
      Nil
    } else {
      val analyzer = TimelinessAnalyzer(timelinessExpr, sourceName)
      val btsSel = analyzer.btsExpr
      val etsSelOpt = analyzer.etsExprOpt

      // 1. in time
      val inTimeTableName = "__inTime"
      val inTimeSql = etsSelOpt match {
        case Some(etsSel) =>
          s"""
             |SELECT *, ($btsSel) AS `${ConstantColumns.beginTs}`,
             |($etsSel) AS `${ConstantColumns.endTs}`
             |FROM $sourceName WHERE ($btsSel) IS NOT NULL AND ($etsSel) IS NOT NULL
           """.stripMargin
        case _ =>
          s"""
             |SELECT *, ($btsSel) AS `${ConstantColumns.beginTs}`
             |FROM $sourceName WHERE ($btsSel) IS NOT NULL
           """.stripMargin
      }
      val inTimeTransStep = SparkSqlTransformStep(inTimeTableName, inTimeSql, emptyMap)

      // 2. latency
      val latencyTableName = "__lat"
      val latencyColName = details.getStringOrKey(_latency)
      val etsColName = etsSelOpt match {
        case Some(_) => ConstantColumns.endTs
        case _ => ConstantColumns.tmst
      }
      val latencySql = {
        s"SELECT *, (`$etsColName` - `${ConstantColumns.beginTs}`) AS `$latencyColName` " +
          s"FROM `$inTimeTableName`"
      }
      val latencyTransStep =
        SparkSqlTransformStep(latencyTableName, latencySql, emptyMap, None, cache = true)
      latencyTransStep.parentSteps += inTimeTransStep

      // 3. timeliness metric
      val metricTableName = ruleParam.getOutDfName()
      val totalColName = details.getStringOrKey(_total)
      val avgColName = details.getStringOrKey(_avg)
      val metricSql = procType match {

        case BatchProcessType =>
          s"""
             |SELECT COUNT(*) AS `$totalColName`,
             |CAST(AVG(`$latencyColName`) AS BIGINT) AS `$avgColName`
             |FROM `$latencyTableName`
           """.stripMargin

        case StreamingProcessType =>
          s"""
             |SELECT `${ConstantColumns.tmst}`,
             |COUNT(*) AS `$totalColName`,
             |CAST(AVG(`$latencyColName`) AS BIGINT) AS `$avgColName`
             |FROM `$latencyTableName`
             |GROUP BY `${ConstantColumns.tmst}`
           """.stripMargin
      }
      val metricWriteStep = {
        val metricOpt = ruleParam.getOutputOpt(MetricOutputType)
        val mwName = metricOpt.flatMap(_.getNameOpt).getOrElse(ruleParam.getOutDfName())
        val flattenType = metricOpt.map(_.getFlatten).getOrElse(DefaultFlattenType)
        MetricWriteStep(mwName, metricTableName, flattenType)
      }
      val metricTransStep =
        SparkSqlTransformStep(metricTableName, metricSql, emptyMap, Some(metricWriteStep))
      metricTransStep.parentSteps += latencyTransStep

      // current steps
      val transSteps1 = metricTransStep :: Nil

      // 4. timeliness record
      val transSteps2 = TimeUtil.milliseconds(details.getString(_threshold, "")) match {
        case Some(tsh) =>
          val recordTableName = "__lateRecords"
          val recordSql = {
            s"SELECT * FROM `$latencyTableName` WHERE `$latencyColName` > $tsh"
          }
          val recordWriteStep = {
            val rwName =
              ruleParam
                .getOutputOpt(RecordOutputType)
                .flatMap(_.getNameOpt)
                .getOrElse(recordTableName)

            RecordWriteStep(rwName, recordTableName, None)
          }
          val recordTransStep =
            SparkSqlTransformStep(recordTableName, recordSql, emptyMap, Some(recordWriteStep))
          recordTransStep.parentSteps += latencyTransStep

          recordTransStep :: Nil
        case _ => Nil
      }

      // 5. ranges
      val transSteps3 = TimeUtil.milliseconds(details.getString(_stepSize, "")) match {
        case Some(stepSize) =>
          // 5.1 range
          val rangeTableName = "__range"
          val stepColName = details.getStringOrKey(_step)
          val rangeSql = {
            s"""
               |SELECT *, CAST((`$latencyColName` / $stepSize) AS BIGINT) AS `$stepColName`
               |FROM `$latencyTableName`
             """.stripMargin
          }
          val rangeTransStep = SparkSqlTransformStep(rangeTableName, rangeSql, emptyMap)
          rangeTransStep.parentSteps += latencyTransStep

          // 5.2 range metric
          val rangeMetricTableName = "__rangeMetric"
          val countColName = details.getStringOrKey(_count)
          val rangeMetricSql = procType match {
            case BatchProcessType =>
              s"""
                 |SELECT `$stepColName`, COUNT(*) AS `$countColName`
                 |FROM `$rangeTableName` GROUP BY `$stepColName`
                """.stripMargin
            case StreamingProcessType =>
              s"""
                 |SELECT `${ConstantColumns.tmst}`, `$stepColName`, COUNT(*) AS `$countColName`
                 |FROM `$rangeTableName` GROUP BY `${ConstantColumns.tmst}`, `$stepColName`
                """.stripMargin
          }
          val rangeMetricWriteStep = {
            MetricWriteStep(stepColName, rangeMetricTableName, ArrayFlattenType)
          }
          val rangeMetricTransStep =
            SparkSqlTransformStep(
              rangeMetricTableName,
              rangeMetricSql,
              emptyMap,
              Some(rangeMetricWriteStep))
          rangeMetricTransStep.parentSteps += rangeTransStep

          rangeMetricTransStep :: Nil
        case _ => Nil
      }

      // 6. percentiles
      val percentiles = getPercentiles(details)
      val transSteps4 = if (percentiles.nonEmpty) {
        val percentileTableName = "__percentile"
        val percentileColName = details.getStringOrKey(_percentileColPrefix)
        val percentileCols = percentiles
          .map { pct =>
            val pctName = (pct * 100).toInt.toString
            s"floor(percentile_approx($latencyColName, $pct)) " +
              s"AS `${percentileColName}_$pctName`"
          }
          .mkString(", ")
        val percentileSql = {
          s"""
             |SELECT $percentileCols
             |FROM `$latencyTableName`
            """.stripMargin
        }
        val percentileWriteStep = {
          MetricWriteStep(percentileTableName, percentileTableName, DefaultFlattenType)
        }
        val percentileTransStep =
          SparkSqlTransformStep(
            percentileTableName,
            percentileSql,
            emptyMap,
            Some(percentileWriteStep))
        percentileTransStep.parentSteps += latencyTransStep

        percentileTransStep :: Nil
      } else Nil

      // full steps
      transSteps1 ++ transSteps2 ++ transSteps3 ++ transSteps4
    }
  }

  private def getPercentiles(details: Map[String, Any]): Seq[Double] = {
    details.getDoubleArr(_percentileValues).filter(d => d >= 0 && d <= 1)
  }

}
