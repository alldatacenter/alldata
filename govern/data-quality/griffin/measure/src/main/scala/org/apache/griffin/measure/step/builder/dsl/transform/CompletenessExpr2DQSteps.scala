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

import org.apache.commons.lang.StringUtils

import org.apache.griffin.measure.configuration.dqdefinition.{RuleErrorConfParam, RuleParam}
import org.apache.griffin.measure.configuration.enums.FlattenType.DefaultFlattenType
import org.apache.griffin.measure.configuration.enums.OutputType._
import org.apache.griffin.measure.configuration.enums.ProcessType._
import org.apache.griffin.measure.context.DQContext
import org.apache.griffin.measure.step.DQStep
import org.apache.griffin.measure.step.builder.ConstantColumns
import org.apache.griffin.measure.step.builder.dsl.expr._
import org.apache.griffin.measure.step.builder.dsl.transform.analyzer.CompletenessAnalyzer
import org.apache.griffin.measure.step.transform.SparkSqlTransformStep
import org.apache.griffin.measure.step.write.{MetricWriteStep, RecordWriteStep}
import org.apache.griffin.measure.utils.ParamUtil._

/**
 * generate completeness dq steps
 */
case class CompletenessExpr2DQSteps(context: DQContext, expr: Expr, ruleParam: RuleParam)
    extends Expr2DQSteps {

  private object CompletenessKeys {
    val _source = "source"
    val _total = "total"
    val _complete = "complete"
    val _incomplete = "incomplete"
  }
  import CompletenessKeys._

  def getDQSteps: Seq[DQStep] = {
    val details = ruleParam.getDetails
    val completenessExpr = expr.asInstanceOf[CompletenessClause]

    val sourceName = details.getString(_source, context.getDataSourceName(0))

    val procType = context.procType
    val timestamp = context.contextId.timestamp

    if (!context.runTimeTableRegister.existsTable(sourceName)) {
      warn(s"[$timestamp] data source $sourceName not exists")
      Nil
    } else {
      val analyzer = CompletenessAnalyzer(completenessExpr, sourceName)

      val selItemsClause = analyzer.selectionPairs
        .map { pair =>
          val (expr, alias) = pair
          s"${expr.desc} AS `$alias`"
        }
        .mkString(", ")
      val aliases = analyzer.selectionPairs.map(_._2)

      val selClause = procType match {
        case BatchProcessType => selItemsClause
        case StreamingProcessType => s"`${ConstantColumns.tmst}`, $selItemsClause"
      }

      // 1. source alias
      val sourceAliasTableName = "__sourceAlias"
      val sourceAliasSql = {
        s"SELECT $selClause FROM `$sourceName`"
      }
      val sourceAliasTransStep =
        SparkSqlTransformStep(sourceAliasTableName, sourceAliasSql, emptyMap, None, cache = true)

      // 2. incomplete record
      val incompleteRecordsTableName = "__incompleteRecords"
      val errorConfs: Seq[RuleErrorConfParam] = ruleParam.getErrorConfs
      var incompleteWhereClause: String = ""
      if (errorConfs.isEmpty) {
        // without errorConfs
        val completeWhereClause = aliases.map(a => s"`$a` IS NOT NULL").mkString(" AND ")
        incompleteWhereClause = s"NOT ($completeWhereClause)"
      } else {
        // with errorConfs
        incompleteWhereClause = this.getErrorConfCompleteWhereClause(errorConfs)
      }

      val incompleteRecordsSql =
        s"SELECT * FROM `$sourceAliasTableName` WHERE $incompleteWhereClause"

      val incompleteRecordWriteStep = {
        val rwName =
          ruleParam
            .getOutputOpt(RecordOutputType)
            .flatMap(_.getNameOpt)
            .getOrElse(incompleteRecordsTableName)
        RecordWriteStep(rwName, incompleteRecordsTableName)
      }
      val incompleteRecordTransStep =
        SparkSqlTransformStep(
          incompleteRecordsTableName,
          incompleteRecordsSql,
          emptyMap,
          Some(incompleteRecordWriteStep),
          cache = true)
      incompleteRecordTransStep.parentSteps += sourceAliasTransStep

      // 3. incomplete count
      val incompleteCountTableName = "__incompleteCount"
      val incompleteColName = details.getStringOrKey(_incomplete)
      val incompleteCountSql = procType match {
        case BatchProcessType =>
          s"SELECT COUNT(*) AS `$incompleteColName` FROM `$incompleteRecordsTableName`"
        case StreamingProcessType =>
          s"SELECT `${ConstantColumns.tmst}`, COUNT(*) AS `$incompleteColName` " +
            s"FROM `$incompleteRecordsTableName` GROUP BY `${ConstantColumns.tmst}`"
      }
      val incompleteCountTransStep =
        SparkSqlTransformStep(incompleteCountTableName, incompleteCountSql, emptyMap)
      incompleteCountTransStep.parentSteps += incompleteRecordTransStep

      // 4. total count
      val totalCountTableName = "__totalCount"
      val totalColName = details.getStringOrKey(_total)
      val totalCountSql = procType match {
        case BatchProcessType =>
          s"SELECT COUNT(*) AS `$totalColName` FROM `$sourceAliasTableName`"
        case StreamingProcessType =>
          s"SELECT `${ConstantColumns.tmst}`, COUNT(*) AS `$totalColName` " +
            s"FROM `$sourceAliasTableName` GROUP BY `${ConstantColumns.tmst}`"
      }
      val totalCountTransStep =
        SparkSqlTransformStep(totalCountTableName, totalCountSql, emptyMap)
      totalCountTransStep.parentSteps += sourceAliasTransStep

      // 5. complete metric
      val completeTableName = ruleParam.getOutDfName()
      val completeColName = details.getStringOrKey(_complete)
      // scalastyle:off
      val completeMetricSql = procType match {
        case BatchProcessType =>
          s"""
             |SELECT `$totalCountTableName`.`$totalColName` AS `$totalColName`,
             |coalesce(`$incompleteCountTableName`.`$incompleteColName`, 0) AS `$incompleteColName`,
             |(`$totalCountTableName`.`$totalColName` - coalesce(`$incompleteCountTableName`.`$incompleteColName`, 0)) AS `$completeColName`
             |FROM `$totalCountTableName` LEFT JOIN `$incompleteCountTableName`
         """.stripMargin
        case StreamingProcessType =>
          s"""
             |SELECT `$totalCountTableName`.`${ConstantColumns.tmst}` AS `${ConstantColumns.tmst}`,
             |`$totalCountTableName`.`$totalColName` AS `$totalColName`,
             |coalesce(`$incompleteCountTableName`.`$incompleteColName`, 0) AS `$incompleteColName`,
             |(`$totalCountTableName`.`$totalColName` - coalesce(`$incompleteCountTableName`.`$incompleteColName`, 0)) AS `$completeColName`
             |FROM `$totalCountTableName` LEFT JOIN `$incompleteCountTableName`
             |ON `$totalCountTableName`.`${ConstantColumns.tmst}` = `$incompleteCountTableName`.`${ConstantColumns.tmst}`
         """.stripMargin
      }
      // scalastyle:on
      val completeWriteStep = {
        val metricOpt = ruleParam.getOutputOpt(MetricOutputType)
        val mwName = metricOpt.flatMap(_.getNameOpt).getOrElse(completeTableName)
        val flattenType = metricOpt.map(_.getFlatten).getOrElse(DefaultFlattenType)
        MetricWriteStep(mwName, completeTableName, flattenType)
      }
      val completeTransStep =
        SparkSqlTransformStep(
          completeTableName,
          completeMetricSql,
          emptyMap,
          Some(completeWriteStep))
      completeTransStep.parentSteps += incompleteCountTransStep
      completeTransStep.parentSteps += totalCountTransStep

      val transSteps = completeTransStep :: Nil
      transSteps
    }
  }

  /**
   * get 'error' where clause
   * @param errorConfs error configuraion list
   * @return 'error' where clause
   */
  def getErrorConfCompleteWhereClause(errorConfs: Seq[RuleErrorConfParam]): String = {
    errorConfs.map(errorConf => this.getEachErrorWhereClause(errorConf)).mkString(" OR ")
  }

  /**
   * get error sql for each column
   * @param errorConf  error configuration
   * @return 'error' sql for each column
   */
  def getEachErrorWhereClause(errorConf: RuleErrorConfParam): String = {
    val errorType: Option[String] = errorConf.getErrorType
    val columnName: String = errorConf.getColumnName.get
    if ("regex".equalsIgnoreCase(errorType.get)) {
      // only have one regular expression
      val regexValue: String = errorConf.getValues.head
      val afterReplace: String = regexValue.replaceAll("""\\""", """\\\\""")
      return s"(`$columnName` REGEXP '$afterReplace')"
    } else if ("enumeration".equalsIgnoreCase(errorType.get)) {
      val values: Seq[String] = errorConf.getValues
      var inResult = ""
      var nullResult = ""
      if (values.contains("hive_none")) {
        // hive_none means NULL
        nullResult = s"`$columnName` IS NULL"
      }

      val valueWithQuote: String = values
        .filter(value => !"hive_none".equals(value))
        .map(value => s"'$value'")
        .mkString(", ")

      if (!StringUtils.isEmpty(valueWithQuote)) {
        inResult = s"`$columnName` IN ($valueWithQuote)"
      }

      var result = ""
      if (!StringUtils.isEmpty(inResult) && !StringUtils.isEmpty(nullResult)) {
        result = s"($inResult OR $nullResult)"
      } else if (!StringUtils.isEmpty(inResult)) {
        result = s"($inResult)"
      } else {
        result = s"($nullResult)"
      }

      return result
    }
    throw new IllegalArgumentException(
      "type in error.confs only supports regex and enumeration way")
  }
}
