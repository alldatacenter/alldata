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

import org.apache.griffin.measure.Loggable
import org.apache.griffin.measure.configuration.dqdefinition.RuleParam
import org.apache.griffin.measure.configuration.enums.DqType._
import org.apache.griffin.measure.context.DQContext
import org.apache.griffin.measure.step.DQStep
import org.apache.griffin.measure.step.builder.dsl.expr.Expr

trait Expr2DQSteps extends Loggable with Serializable {

  protected val emtptDQSteps: Seq[DQStep] = Seq[DQStep]()
  protected val emptyMap: Map[String, Any] = Map[String, Any]()

  def getDQSteps: Seq[DQStep]
}

/**
 * get dq steps generator for griffin dsl rule
 */
object Expr2DQSteps {
  private val emtptExpr2DQSteps: Expr2DQSteps = new Expr2DQSteps {
    def getDQSteps: Seq[DQStep] = emtptDQSteps
  }

  def apply(context: DQContext, expr: Expr, ruleParam: RuleParam): Expr2DQSteps = {
    ruleParam.getDqType match {
      case Accuracy => AccuracyExpr2DQSteps(context, expr, ruleParam)
      case Profiling => ProfilingExpr2DQSteps(context, expr, ruleParam)
      case Uniqueness => UniquenessExpr2DQSteps(context, expr, ruleParam)
      case Distinct => DistinctnessExpr2DQSteps(context, expr, ruleParam)
      case Timeliness => TimelinessExpr2DQSteps(context, expr, ruleParam)
      case Completeness => CompletenessExpr2DQSteps(context, expr, ruleParam)
      case _ => emtptExpr2DQSteps
    }
  }
}
