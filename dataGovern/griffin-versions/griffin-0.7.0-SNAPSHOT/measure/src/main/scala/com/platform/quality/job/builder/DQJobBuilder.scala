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

package com.platform.quality.job.builder

import com.platform.quality.configuration.dqdefinition.{EvaluateRuleParam, RuleParam}
import com.platform.quality.context.DQContext
import com.platform.quality.job
import com.platform.quality.job.DQJob
import com.platform.quality.step.builder.DQStepBuilder
import com.platform.quality.step.write.MetricFlushStep
import org.apache.griffin.measure.configuration.dqdefinition._
import org.apache.griffin.measure.job._

/**
 * build dq job based on configuration
 */
object DQJobBuilder {

  /**
   * build dq job with rule param
   * @param context              dq context
   * @param evaluateRuleParam    evaluate rule param
   * @return       dq job
   */
  def buildDQJob(context: DQContext, evaluateRuleParam: EvaluateRuleParam): DQJob = {
    val ruleParams = evaluateRuleParam.getRules
    buildDQJob(context, ruleParams)
  }

  /**
   * build dq job with rules in evaluate rule param or pre-proc param
   * @param context          dq context
   * @param ruleParams       rule params
   * @return       dq job
   */
  def buildDQJob(context: DQContext, ruleParams: Seq[RuleParam]): DQJob = {
    // build steps by datasources
    val dsSteps = context.dataSources.flatMap { dataSource =>
      DQStepBuilder.buildStepOptByDataSourceParam(context, dataSource.dsParam)
    }
    // build steps by rules
    val ruleSteps = ruleParams.flatMap { ruleParam =>
      DQStepBuilder.buildStepOptByRuleParam(context, ruleParam)
    }
    // metric flush step
    val metricFlushStep = MetricFlushStep()

    job.DQJob(dsSteps ++ ruleSteps :+ metricFlushStep)
  }

}
