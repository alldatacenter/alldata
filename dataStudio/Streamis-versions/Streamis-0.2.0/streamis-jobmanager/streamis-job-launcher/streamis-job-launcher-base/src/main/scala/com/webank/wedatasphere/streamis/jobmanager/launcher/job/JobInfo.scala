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

package com.webank.wedatasphere.streamis.jobmanager.launcher.job

import com.webank.wedatasphere.streamis.jobmanager.launcher.job.state.{JobState, JobStateInfo}

/**
 * Basic job information
 */
trait JobInfo {

  /**
   * Job name
   * @return name
   */
  def getName: String
  /**
   * Job Id
   * @return
   */
  def getId: String

  /**
   * Creator
   * @return
   */
  def getUser: String

  /**
   * Job status
   * @return
   */
  def getStatus: String

  def setStatus(status: String): Unit

  /**
   * Job log path
   * @return
   */
  def getLogPath: String

  def getResources: java.util.Map[String, Object]

  def getCompletedMsg: String

  /**
   * Contains the check point and save points
   * @return
   */
  def getJobStates: Array[JobStateInfo]

}
