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
import org.apache.linkis.manager.label.entity.engine.RunType
import org.apache.linkis.manager.label.entity.engine.RunType.RunType
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.{StreamJob, StreamJobVersion}
import com.webank.wedatasphere.streamis.jobmanager.manager.exception.JobExecuteErrorException
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.entity.{StreamisSqlTransformJobContent, StreamisTransformJobContent}
import org.springframework.stereotype.Component

/**
  * Created by enjoyyin on 2021/9/23.
  */
@Component
class FlinkSQLJobContentParser extends AbstractJobContentParser {

  override def parseTo(job: StreamJob, jobVersion: StreamJobVersion): StreamisTransformJobContent = {
    val jobContent = JsonUtils.jackson.readValue(jobVersion.getJobContent, classOf[util.Map[String, Object]])
    val transformJobContent = new StreamisSqlTransformJobContent
    val sql = jobContent.get("type") match {
      case "file" =>
        jobContent.get("file") match {
          case file: String =>
            getFileContent(job, jobVersion, file)
          case _ => throw new JobExecuteErrorException(30500, s"No file is exists when the type is file in jobContent.")
        }
      case "bml" =>
        val resourceId = jobContent.get("resourceId")
        val version = jobContent.get("version")
        if(resourceId == null || version == null)
          throw new JobExecuteErrorException(30500, s"No resourceId or version is exists when the type is bml in jobContent.")
        readFileFromBML(jobVersion.getCreateBy, resourceId.toString, version.toString)
      case "sql" =>
        jobContent.get("sql") match {
          case sql: String => sql
          case _ => throw new JobExecuteErrorException(30500, s"No sql is exists when the type is sql in jobContent.")
        }
      case t => throw new JobExecuteErrorException(30500, s"Not recognized type $t in jobContent.")
    }
    transformJobContent.setSql(sql)
    transformJobContent
  }

  override val jobType: String = FlinkSQLJobContentParser.JOB_TYPE
  override val runType: RunType = RunType.SQL

}
object FlinkSQLJobContentParser {

  val JOB_TYPE = "flink.sql"

}