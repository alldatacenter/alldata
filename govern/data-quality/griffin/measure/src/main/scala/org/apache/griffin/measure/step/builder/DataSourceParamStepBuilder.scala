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

import org.apache.griffin.measure.configuration.dqdefinition.{DataConnectorParam, DataSourceParam}
import org.apache.griffin.measure.context.DQContext
import org.apache.griffin.measure.step.DQStep
import org.apache.griffin.measure.step.read._

/**
 * build dq step by data source param
 */
trait DataSourceParamStepBuilder extends DQStepBuilder {

  type ParamType = DataSourceParam

  def buildDQStep(context: DQContext, param: ParamType): Option[DQStep] = {
    val name = getStepName(param.getName)

    param.getConnector match {
      case Some(dc) =>
        val steps = buildReadSteps(context, dc)
        if (steps.isDefined) Some(UnionReadStep(name, Seq(steps.get)))
        else None
      case _ => None
    }
  }

  protected def buildReadSteps(context: DQContext, dcParam: DataConnectorParam): Option[ReadStep]

}
