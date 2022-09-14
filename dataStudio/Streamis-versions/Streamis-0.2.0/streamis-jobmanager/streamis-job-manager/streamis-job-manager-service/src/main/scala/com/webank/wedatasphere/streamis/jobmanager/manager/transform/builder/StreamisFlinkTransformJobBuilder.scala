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

package com.webank.wedatasphere.streamis.jobmanager.manager.transform.builder

import org.apache.linkis.manager.label.entity.engine.RunType.RunType
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamJob
import com.webank.wedatasphere.streamis.jobmanager.manager.exception.JobExecuteErrorException
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.JobContentParser
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.entity.{StreamisTransformJob, StreamisTransformJobContent}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component


@Component
class StreamisFlinkTransformJobBuilder extends AbstractFlinkStreamisTransformJobBuilder {

  @Autowired private var jobContentParsers: Array[JobContentParser] = _

  override def canBuild(streamJob: StreamJob): Boolean = jobContentParsers.map(_.jobType).contains(streamJob.getJobType.toLowerCase)

  override protected def getRunType(transformJob: StreamisTransformJob): RunType =
    jobContentParsers.find(_.jobType == transformJob.getStreamJob.getJobType.toLowerCase).map(_.runType).get

  override protected def createStreamisTransformJobContent(transformJob: StreamisTransformJob): StreamisTransformJobContent =
    jobContentParsers.find(_.canParse(transformJob.getStreamJob, transformJob.getStreamJobVersion))
      .map(_.parseTo(transformJob.getStreamJob, transformJob.getStreamJobVersion))
    .getOrElse(throw new JobExecuteErrorException(30350, "Not support jobContent " + transformJob.getStreamJobVersion.getJobContent))
}
