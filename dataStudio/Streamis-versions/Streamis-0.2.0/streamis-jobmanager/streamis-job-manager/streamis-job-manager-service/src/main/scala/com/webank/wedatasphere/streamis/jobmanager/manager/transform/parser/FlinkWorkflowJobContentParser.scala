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

package com.webank.wedatasphere.streamis.jobmanager.manager.transform.parser

import java.util

import org.apache.linkis.common.utils.JsonUtils
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.{StreamJob, StreamJobVersion}
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.entity.{StreamisSqlTransformJobContent, StreamisTransformJobContent, StreamisWorkflowTransformJobContent}
import org.springframework.stereotype.Component

/**
  * Created by enjoyyin on 2021/9/23.
  */
@Component
class FlinkWorkflowJobContentParser extends FlinkSQLJobContentParser {

  override def parseTo(job: StreamJob, jobVersion: StreamJobVersion): StreamisTransformJobContent = super.parseTo(job, jobVersion) match {
    case transformJobContent: StreamisSqlTransformJobContent =>
      val workflowJobContent = new StreamisWorkflowTransformJobContent
      val jobContent = JsonUtils.jackson.readValue(jobVersion.getJobContent, classOf[util.Map[String, Object]])
      workflowJobContent.setWorkflowId(jobContent.get("workflowId").asInstanceOf[Long])
      workflowJobContent.setWorkflowName(jobContent.get("workflowName").asInstanceOf[String])
      workflowJobContent.setSql(transformJobContent.getSql)
      workflowJobContent
  }

  override val jobType: String = FlinkWorkflowJobContentParser.JOB_TYPE
}

object FlinkWorkflowJobContentParser {

  val JOB_TYPE = "flink.workflow"

  def sqlToJobContent(workflowId: java.lang.Long, workflowName: String, sql: String): util.Map[String, Object] = {
    val jobContent = new util.HashMap[String, Object]
    jobContent.put("type", "sql")
    jobContent.put("sql", sql)
    jobContent.put("workflowId", workflowId)
    jobContent.put("workflowName", workflowName)
    jobContent
  }

}