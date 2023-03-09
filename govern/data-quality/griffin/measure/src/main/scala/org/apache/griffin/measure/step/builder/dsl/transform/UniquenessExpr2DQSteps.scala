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
  EntriesFlattenType
}
import org.apache.griffin.measure.configuration.enums.OutputType._
import org.apache.griffin.measure.configuration.enums.ProcessType._
import org.apache.griffin.measure.context.DQContext
import org.apache.griffin.measure.step.DQStep
import org.apache.griffin.measure.step.builder.ConstantColumns
import org.apache.griffin.measure.step.builder.dsl.expr._
import org.apache.griffin.measure.step.builder.dsl.transform.analyzer.UniquenessAnalyzer
import org.apache.griffin.measure.step.transform.SparkSqlTransformStep
import org.apache.griffin.measure.step.write.{MetricWriteStep, RecordWriteStep}
import org.apache.griffin.measure.utils.ParamUtil._

/**
 * generate uniqueness dq steps
 */
case class UniquenessExpr2DQSteps(context: DQContext, expr: Expr, ruleParam: RuleParam)
    extends Expr2DQSteps {

  private object UniquenessKeys {
    val _source = "source"
    val _target = "target"
    val _unique = "unique"
    val _total = "total"
    val _dup = "dup"
    val _num = "num"

    val _duplicationArray = "duplication.array"
  }
  import UniquenessKeys._

  def getDQSteps: Seq[DQStep] = {
    val details = ruleParam.getDetails
    val uniquenessExpr = expr.asInstanceOf[UniquenessClause]

    val sourceName = details.getString(_source, context.getDataSourceName(0))
    val targetName = details.getString(_target, context.getDataSourceName(1))
    val analyzer = UniquenessAnalyzer(uniquenessExpr, sourceName, targetName)

    val procType = context.procType
    val timestamp = context.contextId.timestamp

    if (!context.runTimeTableRegister.existsTable(sourceName)) {
      warn(s"[$timestamp] data source $sourceName not exists")
      Nil
    } else if (!context.runTimeTableRegister.existsTable(targetName)) {
      warn(s"[$timestamp] data source $targetName not exists")
      Nil
    } else {
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
      val selAliases = procType match {
        case BatchProcessType => aliases
        case StreamingProcessType => ConstantColumns.tmst +: aliases
      }

      // 1. source distinct mapping
      val sourceTableName = "__source"
      val sourceSql = s"SELECT DISTINCT $selClause FROM $sourceName"
      val sourceTransStep = SparkSqlTransformStep(sourceTableName, sourceSql, emptyMap)

      // 2. target mapping
      val targetTableName = "__target"
      val targetSql = s"SELECT $selClause FROM $targetName"
      val targetTransStep = SparkSqlTransformStep(targetTableName, targetSql, emptyMap)

      // 3. joined
      val joinedTableName = "__joined"
      val joinedSelClause = selAliases
        .map { alias =>
          s"`$sourceTableName`.`$alias` AS `$alias`"
        }
        .mkString(", ")
      val onClause = aliases
        .map { alias =>
          s"coalesce(`$sourceTableName`.`$alias`, '') = coalesce(`$targetTableName`.`$alias`, '')"
        }
        .mkString(" AND ")
      val joinedSql = {
        s"SELECT $joinedSelClause FROM `$targetTableName` RIGHT JOIN `$sourceTableName` ON $onClause"
      }
      val joinedTransStep = SparkSqlTransformStep(joinedTableName, joinedSql, emptyMap)
      joinedTransStep.parentSteps += sourceTransStep
      joinedTransStep.parentSteps += targetTransStep

      // 4. group
      val groupTableName = "__group"
      val groupSelClause = selAliases
        .map { alias =>
          s"`$alias`"
        }
        .mkString(", ")
      val dupColName = details.getStringOrKey(_dup)
      val groupSql = {
        s"SELECT $groupSelClause, (COUNT(*) - 1) AS `$dupColName` " +
          s"FROM `$joinedTableName` GROUP BY $groupSelClause"
      }
      val groupTransStep =
        SparkSqlTransformStep(groupTableName, groupSql, emptyMap, None, cache = true)
      groupTransStep.parentSteps += joinedTransStep

      // 5. total metric
      val totalTableName = "__totalMetric"
      val totalColName = details.getStringOrKey(_total)
      val totalSql = procType match {
        case BatchProcessType => s"SELECT COUNT(*) AS `$totalColName` FROM `$sourceName`"
        case StreamingProcessType =>
          s"""
             |SELECT `${ConstantColumns.tmst}`, COUNT(*) AS `$totalColName`
             |FROM `$sourceName` GROUP BY `${ConstantColumns.tmst}`
           """.stripMargin
      }
      val totalMetricWriteStep = MetricWriteStep(totalColName, totalTableName, EntriesFlattenType)
      val totalTransStep =
        SparkSqlTransformStep(totalTableName, totalSql, emptyMap, Some(totalMetricWriteStep))

      // 6. unique record
      val uniqueRecordTableName = "__uniqueRecord"
      val uniqueRecordSql = {
        s"SELECT * FROM `$groupTableName` WHERE `$dupColName` = 0"
      }
      val uniqueRecordTransStep =
        SparkSqlTransformStep(uniqueRecordTableName, uniqueRecordSql, emptyMap)
      uniqueRecordTransStep.parentSteps += groupTransStep

      // 7. unique metric
      val uniqueTableName = "__uniqueMetric"
      val uniqueColName = details.getStringOrKey(_unique)
      val uniqueSql = procType match {
        case BatchProcessType =>
          s"SELECT COUNT(*) AS `$uniqueColName` FROM `$uniqueRecordTableName`"
        case StreamingProcessType =>
          s"""
             |SELECT `${ConstantColumns.tmst}`, COUNT(*) AS `$uniqueColName`
             |FROM `$uniqueRecordTableName` GROUP BY `${ConstantColumns.tmst}`
           """.stripMargin
      }
      val uniqueMetricWriteStep =
        MetricWriteStep(uniqueColName, uniqueTableName, EntriesFlattenType)
      val uniqueTransStep =
        SparkSqlTransformStep(uniqueTableName, uniqueSql, emptyMap, Some(uniqueMetricWriteStep))
      uniqueTransStep.parentSteps += uniqueRecordTransStep

      val transSteps1 = totalTransStep :: uniqueTransStep :: Nil

      val duplicationArrayName = details.getString(_duplicationArray, "")
      val transSteps2 = if (duplicationArrayName.nonEmpty) {
        // 8. duplicate record
        val dupRecordTableName = "__dupRecords"
        val dupRecordSql = {
          s"SELECT * FROM `$groupTableName` WHERE `$dupColName` > 0"
        }

        val dupRecordWriteStep = {
          val rwName =
            ruleParam
              .getOutputOpt(RecordOutputType)
              .flatMap(_.getNameOpt)
              .getOrElse(dupRecordTableName)

          RecordWriteStep(rwName, dupRecordTableName)
        }
        val dupRecordTransStep =
          SparkSqlTransformStep(
            dupRecordTableName,
            dupRecordSql,
            emptyMap,
            Some(dupRecordWriteStep),
            cache = true)

        // 9. duplicate metric
        val dupMetricTableName = "__dupMetric"
        val numColName = details.getStringOrKey(_num)
        val dupMetricSelClause = procType match {
          case BatchProcessType => s"`$dupColName`, COUNT(*) AS `$numColName`"

          case StreamingProcessType =>
            s"`${ConstantColumns.tmst}`, `$dupColName`, COUNT(*) AS `$numColName`"
        }
        val dupMetricGroupbyClause = procType match {
          case BatchProcessType => s"`$dupColName`"
          case StreamingProcessType => s"`${ConstantColumns.tmst}`, `$dupColName`"
        }
        val dupMetricSql = {
          s"""
             |SELECT $dupMetricSelClause FROM `$dupRecordTableName`
             |GROUP BY $dupMetricGroupbyClause
          """.stripMargin
        }
        val dupMetricWriteStep = {
          MetricWriteStep(duplicationArrayName, dupMetricTableName, ArrayFlattenType)
        }
        val dupMetricTransStep =
          SparkSqlTransformStep(
            dupMetricTableName,
            dupMetricSql,
            emptyMap,
            Some(dupMetricWriteStep))
        dupMetricTransStep.parentSteps += dupRecordTransStep

        dupMetricTransStep :: Nil
      } else Nil

      // full steps
      transSteps1 ++ transSteps2
    }
  }

}
