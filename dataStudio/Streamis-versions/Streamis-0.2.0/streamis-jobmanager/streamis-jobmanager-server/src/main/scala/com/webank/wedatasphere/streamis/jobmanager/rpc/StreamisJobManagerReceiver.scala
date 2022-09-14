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

package com.webank.wedatasphere.streamis.jobmanager.rpc

import org.apache.linkis.common.conf.CommonVars
import org.apache.linkis.common.utils.Logging
import org.apache.linkis.rpc.{Receiver, Sender}
import com.webank.wedatasphere.streamis.jobmanager.common.protocol.{ImportJobManagerRequest, ImportJobManagerResponse}
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.MetaJsonInfo
import com.webank.wedatasphere.streamis.jobmanager.manager.service.{DefaultStreamJobService, StreamJobService}
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.parser.{FlinkSQLJobContentParser, FlinkWorkflowJobContentParser}
import org.apache.commons.lang.StringUtils

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration

/**
 * created by cooperyang on 2021/7/19
 * Description:
 */
class StreamisJobManagerReceiver(jobService: StreamJobService) extends Receiver with Logging {


  private val timeout = CommonVars("wds.streamis.workflow.ask.timeout", 300).getValue


  override def receive(message: Any, sender: Sender): Unit = {

  }

  override def receiveAndReply(message: Any, sender: Sender): Any = receiveAndReply(message, Duration(timeout, "seconds"), sender)


  override def receiveAndReply(message: Any, duration: Duration, sender: Sender): Any = message match {
    case request: ImportJobManagerRequest =>
      info(s"Try to publish DSS tasks with $request.")
      val metaJsonInfo = new MetaJsonInfo
      metaJsonInfo.setWorkspaceName(request.workspaceName)
      metaJsonInfo.setDescription(request.description)
      metaJsonInfo.setProjectName(request.projectName)
      metaJsonInfo.setJobContent(FlinkWorkflowJobContentParser.sqlToJobContent(request.workflowId, request.workflowName, request.executionCode))
      metaJsonInfo.setJobType(FlinkSQLJobContentParser.JOB_TYPE)
      metaJsonInfo.setJobName(request.streamJobName)
      if (StringUtils.isNotEmpty(request.version)) metaJsonInfo.setComment("Published from DSS with " + request.version)
      metaJsonInfo.setTags(request.tags.asScala.mkString(","))
      val streamJobVersion = jobService.createOrUpdate(request.publishUser, metaJsonInfo)
      ImportJobManagerResponse(0, streamJobVersion.getJobId, "Publish succeed.")
    case _ =>
  }


}
