/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.operator

import org.apache.linkis.computation.client.once.action.EngineConnOperateAction
import org.apache.linkis.computation.client.operator.impl.EngineConnLogOperator

/**
 * Extend the engine conn log operator
 */
class FlinkYarnLogOperator extends EngineConnLogOperator{

  private var applicationId: String = _

  def setApplicationId(applicationId: String): Unit = {
    this.applicationId = applicationId
  }

  protected override def addParameters(builder: EngineConnOperateAction.Builder): Unit = {
    builder.addParameter("yarnApplicationId", this.applicationId)
    super.addParameters(builder)
  }

  override def getName: String = FlinkYarnLogOperator.OPERATOR_NAME
}

object FlinkYarnLogOperator{
  val OPERATOR_NAME = "engineConnYarnLog"
}
