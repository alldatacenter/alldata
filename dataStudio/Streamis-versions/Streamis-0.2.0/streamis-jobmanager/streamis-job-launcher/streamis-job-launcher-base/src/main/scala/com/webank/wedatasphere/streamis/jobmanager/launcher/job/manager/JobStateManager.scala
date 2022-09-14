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

package com.webank.wedatasphere.streamis.jobmanager.launcher.job.manager
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.JobInfo
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.state.{JobState, JobStateFetcher}

import java.net.URI

/**
 * Job state manager
 */
trait JobStateManager {

  /**
   * Init method
   */
  def init(): Unit

  /**
   * Destroy method
   */
  def destroy(): Unit

  /**
   * Register job state fetcher
   * @param clazz clazz
   * @param builder job state fetcher
   * @tparam T
   */
  def registerJobStateFetcher(clazz: Class[_],  builder: () => JobStateFetcher[_ <: JobState]): Unit
  /**
   * Job state fetcher
   * @param clazz clazz
   * @tparam T name
   * @return
   */
  def getOrCreateJobStateFetcher[T <: JobState](clazz: Class[_]): JobStateFetcher[T]

  /**
   * Get job state
   * @param jobInfo job info
   * @tparam T name
   * @return
   */
  def getJobState[T <: JobState](clazz: Class[_], jobInfo: JobInfo): T


  def getJobStateDir[T <: JobState](clazz: Class[_], scheme: String, relativePath: String): URI

  def getJobStateDir[T <: JobState](clazz: Class[_], relativePath: String): URI
  /**
   * Get job state directory uri
   * @param clazz clazz
   * @param scheme scheme
   * @param authority authority
   * @param relativePath relative path
   * @tparam T
   * @return
   */
  def getJobStateDir[T <: JobState](clazz: Class[_], scheme: String, authority: String, relativePath: String): URI
}


