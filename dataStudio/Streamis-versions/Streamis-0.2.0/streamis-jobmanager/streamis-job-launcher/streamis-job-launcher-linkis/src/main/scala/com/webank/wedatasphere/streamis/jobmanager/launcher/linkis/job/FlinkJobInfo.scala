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

package com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job

import com.webank.wedatasphere.streamis.jobmanager.launcher.job.state.{JobState, JobStateInfo}
import org.apache.linkis.common.ServiceInstance
import org.apache.linkis.httpclient.dws.DWSHttpClient

import java.util


class FlinkJobInfo extends YarnJobInfo {

  private var id: String = _
  private var name: String = _
  private var ecmInstance: ServiceInstance = _
  private var user: String = _
  private var savepoint: String = _
  private var checkpoint: String = _
  private var applicationId: String = _
  private var applicationUrl: String = _
  private var status: String = _
  private var logPath: String = _
  private var resources: java.util.Map[String, Object] = _
  private var completedMsg: String = _
  private var jobStates: Array[JobStateInfo] = _
  override def getApplicationId: String = applicationId
  def setApplicationId(applicationId: String): Unit = this.applicationId = applicationId

  override def getApplicationUrl: String = applicationUrl
  def setApplicationUrl(applicationUrl: String): Unit = this.applicationUrl = applicationUrl

  override def getId: String = id
  def setId(id: String): Unit = this.id = id


  override def getECMInstance: ServiceInstance = ecmInstance
  def setECMInstance(ecmInstance: ServiceInstance): Unit = this.ecmInstance = ecmInstance

  override def getUser: String = user
  def setUser(user: String): Unit = this.user = user

  override def getStatus: String = status
  override def setStatus(status: String): Unit = this.status = status

  override def getLogPath: String = logPath
  def setLogPath(logPath: String): Unit = this.logPath = logPath

  override def getResources: util.Map[String, Object] = resources
  def setResources(resources: java.util.Map[String, Object]): Unit = this.resources = resources

  def getSavepoint: String = savepoint
  def setSavepoint(savepoint: String): Unit = this.savepoint = savepoint

  def getCheckpoint: String = checkpoint
  def setCheckpoint(checkpoint: String): Unit = this.checkpoint = checkpoint

  override def getCompletedMsg: String = completedMsg
  def setCompletedMsg(completedMsg: String): Unit = this.completedMsg = completedMsg

  override def toString: String = s"FlinkJobInfo(id: $id, status: $status, applicationId: $applicationId, applicationUrl: $applicationUrl, logPath: $logPath)"

  /**
   * Contains the check point and save points
   *
   * @return
   */
  override def getJobStates: Array[JobStateInfo] = {
    jobStates
  }

  def setJobStates(jobStates: Array[JobStateInfo]): Unit = {
    this.jobStates = jobStates
  }
  /**
   * Job name
   *
   * @return name
   */
  override def getName: String = name

  def setName(name: String): Unit = {
    this.name = name
  }
}

object FlinkJobInfo{
  def main(args: Array[String]): Unit = {
    val jobInfo = "{\"jobStates:\":{\"location\":\"xx\"}"
    DWSHttpClient.jacksonJson.readValue(jobInfo, classOf[FlinkJobInfo])
  }
}
