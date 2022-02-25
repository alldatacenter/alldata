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

package org.apache.griffin.measure.step.builder

import org.apache.commons.lang.StringUtils

import org.apache.griffin.measure.Loggable
import org.apache.griffin.measure.configuration.dqdefinition.{DataSourceParam, Param, RuleParam}
import org.apache.griffin.measure.configuration.enums.DslType._
import org.apache.griffin.measure.configuration.enums.ProcessType._
import org.apache.griffin.measure.context.DQContext
import org.apache.griffin.measure.step._

/**
 * build dq step by param
 */
trait DQStepBuilder extends Loggable with Serializable {

  type ParamType <: Param

  def buildDQStep(context: DQContext, param: ParamType): Option[DQStep]

  protected def getStepName(name: String): String = {
    if (StringUtils.isNotBlank(name)) name
    else DQStepNameGenerator.genName
  }

}

object DQStepBuilder {

  def buildStepOptByDataSourceParam(
      context: DQContext,
      dsParam: DataSourceParam): Option[DQStep] = {
    getDataSourceParamStepBuilder(context.procType)
      .flatMap(_.buildDQStep(context, dsParam))
  }

  private def getDataSourceParamStepBuilder(
      procType: ProcessType): Option[DataSourceParamStepBuilder] = {
    procType match {
      case BatchProcessType => Some(BatchDataSourceStepBuilder())
      case StreamingProcessType => Some(StreamingDataSourceStepBuilder())
      case _ => None
    }
  }

  def buildStepOptByRuleParam(context: DQContext, ruleParam: RuleParam): Option[DQStep] = {
    val dslType = ruleParam.getDslType
    val dsNames = context.dataSourceNames
    val funcNames = context.functionNames
    val dqStepOpt = getRuleParamStepBuilder(dslType, dsNames, funcNames)
      .flatMap(_.buildDQStep(context, ruleParam))
    dqStepOpt.toSeq
      .flatMap(_.getNames)
      .foreach(name => context.compileTableRegister.registerTable(name))
    dqStepOpt
  }

  private def getRuleParamStepBuilder(
      dslType: DslType,
      dsNames: Seq[String],
      funcNames: Seq[String]): Option[RuleParamStepBuilder] = {
    dslType match {
      case SparkSql => Some(SparkSqlDQStepBuilder())
      case GriffinDsl => Some(GriffinDslDQStepBuilder(dsNames, funcNames))
      case _ => None
    }
  }

}
