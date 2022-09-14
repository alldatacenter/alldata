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

import java.util
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.entity.{StreamisTransformJob, StreamisTransformJobContent}



trait StreamisJobContentTransform extends Transform {

  override def transform(streamisTransformJob: StreamisTransformJob, job: LaunchJob): LaunchJob = {
    val jobContent = transformJobContent(streamisTransformJob.getStreamisTransformJobContent)
    if(jobContent != null) {
      jobContent.put("runType", streamisTransformJob.getStreamisJobEngineConn.getRunType.toString)
      LaunchJob.builder().setLaunchJob(job).setJobContent(jobContent).build()
    } else job
  }


  protected def transformJobContent(transformJob: StreamisTransformJobContent): util.HashMap[String, Any]
}
