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


package com.webank.wedatasphere.streamis.jobmanager.manager.utils

import com.webank.wedatasphere.streamis.jobmanager.launcher.job.JobInfo
import com.webank.wedatasphere.streamis.jobmanager.manager.conf.JobConf
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamTask
import org.apache.commons.lang.StringUtils
import org.apache.linkis.httpclient.dws.DWSHttpClient

import java.util.{Calendar, Date}

/**
 * Utils for stream task
 */
object StreamTaskUtils {

  /**
   * Refresh the task info
   * @param task stream task
   * @param jobInfo job info
   */
  def refreshInfo(task: StreamTask, jobInfo: JobInfo): Unit = {
    val time = Calendar.getInstance.getTime
    task.setLastUpdateTime(time)
    task.setStatus(JobConf.linkisStatusToStreamisStatus(jobInfo.getStatus))
    if(JobConf.isCompleted(task.getStatus) && StringUtils.isNotEmpty(jobInfo.getCompletedMsg))
      task.setErrDesc(jobInfo.getCompletedMsg)
    task.setLinkisJobInfo(DWSHttpClient.jacksonJson.writeValueAsString(jobInfo))
  }
}
