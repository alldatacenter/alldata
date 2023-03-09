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

import org.apache.griffin.measure.configuration.dqdefinition.RuleParam
import org.apache.griffin.measure.configuration.enums.OutputType._
import org.apache.griffin.measure.context.DQContext
import org.apache.griffin.measure.step.{DQStep, SeqDQStep}
import org.apache.griffin.measure.step.write.{
  DataSourceUpdateWriteStep,
  MetricWriteStep,
  RecordWriteStep
}

/**
 * build dq step by rule param
 */
trait RuleParamStepBuilder extends DQStepBuilder {

  type ParamType = RuleParam

  def buildDQStep(context: DQContext, param: ParamType): Option[DQStep] = {
    val steps = buildSteps(context, param)
    if (steps.size > 1) Some(SeqDQStep(steps))
    else if (steps.size == 1) steps.headOption
    else None
  }

  def buildSteps(context: DQContext, ruleParam: RuleParam): Seq[DQStep]

  protected def buildDirectWriteSteps(ruleParam: RuleParam): Seq[DQStep] = {
    val name = getStepName(ruleParam.getOutDfName())
    // metric writer
    val metricSteps = ruleParam
      .getOutputOpt(MetricOutputType)
      .map { metric =>
        MetricWriteStep(metric.getNameOpt.getOrElse(name), name, metric.getFlatten)
      }
      .toSeq
    // record writer
    val recordSteps = ruleParam
      .getOutputOpt(RecordOutputType)
      .map { record =>
        RecordWriteStep(record.getNameOpt.getOrElse(name), name)
      }
      .toSeq
    // update writer
    val dsCacheUpdateSteps = ruleParam
      .getOutputOpt(DscUpdateOutputType)
      .map { dsCacheUpdate =>
        DataSourceUpdateWriteStep(dsCacheUpdate.getNameOpt.getOrElse(""), name)
      }
      .toSeq

    metricSteps ++ recordSteps ++ dsCacheUpdateSteps
  }

}
