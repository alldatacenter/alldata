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
package com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.state

import com.webank.wedatasphere.streamis.jobmanager.launcher.job.state.JobState

import java.net.URI

/**
 * Generic flink job state
 */
class GenericFlinkJobState(location: String) extends JobState{

  private var timestamp: Long = -1

  private var id: String = "{ID}"

  private var metadataInfo: Any = _

  override def getLocation: URI = URI.create(location)

  override def getMetadataInfo: Any = {
    metadataInfo
  }

  def setMetadataInfo(metadataInfo: Any): Unit = {
    this.metadataInfo = metadataInfo
  }

  /**
   * Job state id
   *
   * @return
   */
  override def getId: String = id

  def setId(id: String): Unit = {
      this.id = id
  }
  /**
   * Timestamp to save the state
   *
   * @return
   */
  override def getTimestamp: Long = timestamp

  def setTimestamp(timestamp: Long): Unit = {
    this.timestamp = timestamp
  }
}
