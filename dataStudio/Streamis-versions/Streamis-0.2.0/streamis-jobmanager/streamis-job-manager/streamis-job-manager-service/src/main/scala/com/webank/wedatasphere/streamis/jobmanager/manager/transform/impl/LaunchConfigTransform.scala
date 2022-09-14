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

package com.webank.wedatasphere.streamis.jobmanager.manager.transform.impl

import com.webank.wedatasphere.streamis.jobmanager.launcher.job.LaunchJob

import java.util
import com.webank.wedatasphere.streamis.jobmanager.manager.conf.JobConf
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.Transform
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.entity.StreamisTransformJob
import org.apache.linkis.DataWorkCloudApplication

/**
 *
 * @date 2022-01-08
 * @author enjoyyin
 * @since 0.1.0
 */
class LaunchConfigTransform extends Transform {

  override def transform(streamisTransformJob: StreamisTransformJob, job: LaunchJob): LaunchJob = {
    val launchConfigs = if(job.getLaunchConfigs != null) job.getLaunchConfigs else new util.HashMap[String, Any]
    launchConfigs.putIfAbsent(LaunchJob.LAUNCH_CONFIG_CREATE_SERVICE, DataWorkCloudApplication.getServiceInstance.toString)
    launchConfigs.putIfAbsent(LaunchJob.LAUNCH_CONFIG_DESCRIPTION, streamisTransformJob.getStreamJob.getDescription)
    launchConfigs.putIfAbsent(LaunchJob.LAUNCH_CONFIG_MAX_SUBMIT_TIME, JobConf.TASK_SUBMIT_TIME_MAX.getValue.toLong)
    LaunchJob.builder().setLaunchJob(job).setLaunchConfigs(launchConfigs).build()
  }

}
