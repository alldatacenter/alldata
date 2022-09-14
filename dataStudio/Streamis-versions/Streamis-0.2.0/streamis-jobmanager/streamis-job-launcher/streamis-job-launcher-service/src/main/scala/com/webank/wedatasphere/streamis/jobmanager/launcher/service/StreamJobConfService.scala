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
package com.webank.wedatasphere.streamis.jobmanager.launcher.service

import com.webank.wedatasphere.streamis.jobmanager.launcher.entity.JobConfDefinition
import com.webank.wedatasphere.streamis.jobmanager.launcher.entity.vo.JobConfValueSet

import java.util
/**
 * Job configuration service
 */
trait StreamJobConfService {

  /**
   * Get all config definitions
   * @return list
   */
  def loadAllDefinitions(): util.List[JobConfDefinition]

  /**
   * Save job configuration
   * @param jobId job id
   * @param valueMap value map
   */
  def saveJobConfig(jobId: Long, valueMap: util.Map[String, Any]): Unit

  /**
   * Query the job configuration
   * @param jobId job id
   * @return
   */
  def getJobConfig(jobId: Long): util.Map[String, Any]

  /**
   * Query the job value
   * @param jobId job id
   * @param configKey config key
   * @return
   */
  def getJobConfValue(jobId: Long, configKey: String): String

  /**
   * Get job configuration value set
   * @param jobId job id
   * @return
   */
  def getJobConfValueSet(jobId: Long): JobConfValueSet

  /**
   * Save job configuration value set
   * @param valueSet value set
   */
  def saveJobConfValueSet(valueSet: JobConfValueSet): Unit
}
