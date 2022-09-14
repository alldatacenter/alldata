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

import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.state.Savepoint
import org.apache.linkis.computation.client.once.action.EngineConnOperateAction
import org.apache.linkis.computation.client.once.result.EngineConnOperateResult
import org.apache.linkis.computation.client.operator.OnceJobOperator

/**
 * Flink trigger savepoint operator
 */
class FlinkTriggerSavepointOperator extends OnceJobOperator[Savepoint]{

  /**
   * Save point directory
   */
  private var savepointDir: String = _

  /**
   * Mode
   */
  private var mode: String = _

  def setSavepointDir(savepointDir: String): Unit ={
      this.savepointDir = savepointDir
  }

  def setMode(mode: String): Unit = {
    this.mode = mode
  }

  override protected def addParameters(builder: EngineConnOperateAction.Builder): Unit = {
    builder.addParameter("savepointPath", savepointDir)
    builder.addParameter("mode", mode)
  }

  override protected def resultToObject(result: EngineConnOperateResult): Savepoint = {
      val savepointPath:String = result.getAs("writtenSavepoint")
      info(s"Get the savepoint store path: [$savepointPath] form ${FlinkTriggerSavepointOperator.OPERATOR_NAME} operation")
      new Savepoint(savepointPath)
  }

  override def getName: String = FlinkTriggerSavepointOperator.OPERATOR_NAME
}
object FlinkTriggerSavepointOperator{
  val OPERATOR_NAME = "doSavepoint"
}