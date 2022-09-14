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

package com.webank.wedatasphere.streamis.jobmanager.manager.transform.entity

import org.apache.linkis.manager.label.entity.engine.RunType._


trait StreamisJobEngineConn {

  def getRunType: RunType

  def getEngineConnType: String

}

class StreamisJobEngineConnImpl extends StreamisJobEngineConn {

  private var engineConnType: String = _
  private var runType: RunType = _

  def setRunType(runType: RunType): Unit = this.runType = runType
  override def getRunType: RunType = runType

  override def getEngineConnType: String = engineConnType
  def setEngineConnType(engineConnType: String): Unit = this.engineConnType = engineConnType

}
