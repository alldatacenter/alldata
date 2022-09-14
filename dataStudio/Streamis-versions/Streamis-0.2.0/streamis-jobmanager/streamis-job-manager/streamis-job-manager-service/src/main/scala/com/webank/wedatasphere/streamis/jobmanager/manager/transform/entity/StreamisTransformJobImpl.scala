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

import com.webank.wedatasphere.streamis.jobmanager.manager.entity.{StreamJob, StreamJobVersion}

import java.util


class StreamisTransformJobImpl extends StreamisTransformJob {

  private var streamJob: StreamJob = _
  private var streamJobVersion: StreamJobVersion = _
  private var configMap: util.Map[String, Any] = _
  private var streamisJobEngineConn: StreamisJobEngineConn = _
  private var streamisTransformJobContent: StreamisTransformJobContent = _

  override def getStreamJob: StreamJob = streamJob
  def setStreamJob(streamJob: StreamJob): Unit = this.streamJob = streamJob

  override def getStreamJobVersion: StreamJobVersion = streamJobVersion
  def setStreamJobVersion(streamJobVersion: StreamJobVersion): Unit = this.streamJobVersion = streamJobVersion

  override def getStreamisJobEngineConn: StreamisJobEngineConn = streamisJobEngineConn
  def setStreamisJobEngineConn(streamisJobEngineConn: StreamisJobEngineConn): Unit = this.streamisJobEngineConn = streamisJobEngineConn

  override def getStreamisTransformJobContent: StreamisTransformJobContent = streamisTransformJobContent
  def setStreamisTransformJobContent(streamisTransformJobContent: StreamisTransformJobContent): Unit =
    this.streamisTransformJobContent = streamisTransformJobContent

  override def getConfigMap: util.Map[String, Any] =this.configMap

  def setConfigMap(mapValue: util.Map[String, Any]): Unit = {
    this.configMap = mapValue
  }
}
