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

package com.webank.wedatasphere.streamis.jobmanager.manager.transform

import com.webank.wedatasphere.streamis.jobmanager.launcher.job.LaunchJob
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.entity.StreamisTransformJob
import org.apache.commons.lang3.StringUtils

import java.util

/**
 * Config transform
 */
trait ConfigTransform extends Transform {

  override def transform(streamisTransformJob: StreamisTransformJob, job: LaunchJob): LaunchJob = {
    val config: util.Map[String, Any] = streamisTransformJob.getConfigMap
    val group = configGroup()
    if (StringUtils.isNotBlank(group)){
      Option(config.get(group)) match {
        case Some(valueSet: util.Map[String, Any]) =>
          transform(valueSet, job)
        case _ => job
      }
    } else transform(streamisTransformJob.getConfigMap, job)
  }

  /**
   * Config group name
   * @return
   */
  protected def configGroup(): String = null

  protected def transform(valueSet: util.Map[String, Any], job: LaunchJob): LaunchJob

}