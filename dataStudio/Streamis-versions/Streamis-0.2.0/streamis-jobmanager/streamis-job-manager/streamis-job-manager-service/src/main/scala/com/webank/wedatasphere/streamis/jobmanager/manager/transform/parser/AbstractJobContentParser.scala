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

import java.io.InputStream
import java.util

import org.apache.linkis.common.conf.Configuration
import org.apache.linkis.common.utils.{JsonUtils, Logging}
import com.webank.wedatasphere.streamis.jobmanager.manager.dao.StreamJobMapper
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.{StreamJob, StreamJobVersion, StreamisFile}
import com.webank.wedatasphere.streamis.jobmanager.manager.exception.JobExecuteErrorException
import com.webank.wedatasphere.streamis.jobmanager.manager.service.{BMLService, StreamiFileService}
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.JobContentParser
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired

import scala.collection.JavaConverters._

/**
  * Created by enjoyyin on 2021/9/23.
  */
abstract class AbstractJobContentParser extends JobContentParser with Logging {

  @Autowired private var streamJobMapper: StreamJobMapper = _
  @Autowired private var bmlService: BMLService = _
  @Autowired private var streamiFileService: StreamiFileService = _

  private def findFromProject(projectName: String, fileName: String): StreamisFile = fileName match {
    case AbstractJobContentParser.PROJECT_FILE_REGEX(name, version) =>
      val file = streamiFileService.getFile(projectName, name, version)
      if(file == null)
        throw new JobExecuteErrorException(30500, s"Not exists file $fileName.")
      file
    case _ =>
      val files = streamiFileService.listFileVersions(projectName, fileName)
      if(files == null || files.isEmpty)
        throw new JobExecuteErrorException(30500, s"Not exists file $fileName.")
      files.get(0)
  }

  private def findFromProject(projectName: String, fileNames: Array[String]): Array[StreamisFile] = {
    if(fileNames == null || fileNames.isEmpty) Array.empty
    else fileNames.map(findFromProject(projectName, _))
  }

  protected def findFile(job: StreamJob, jobVersion: StreamJobVersion, fileName: String): StreamisFile = {
    val files = streamJobMapper.getStreamJobVersionFiles(jobVersion.getJobId, jobVersion.getId)
    val (file, fileSource) = if(files == null || files.isEmpty)
      (findFromProject(job.getProjectName, fileName), "project")
    else files.asScala.find(_.getFileName == fileName).map((_, "jobVersion")).getOrElse((findFromProject(job.getProjectName, fileName), "project"))
    info(s"Find a $fileSource file(${file.getFileName}, ${file.getVersion}) with storePath ${file.getStorePath} for StreamJob-${job.getName} with file $fileName.")
    file
  }

  protected def findFiles(job: StreamJob, jobVersion: StreamJobVersion, fileNames: Array[String]): Array[StreamisFile] = {
    val files = streamJobMapper.getStreamJobVersionFiles(jobVersion.getJobId, jobVersion.getId)
    if(files == null || files.isEmpty) findFromProject(job.getProjectName, fileNames)
    else {
      val streamisFiles = files.asScala.filter(fileNames.contains)
      if(streamisFiles.size == fileNames.length) streamisFiles.toArray
      else (streamisFiles ++ findFromProject(job.getProjectName, fileNames.filterNot(files.contains))).toArray
    }
  }

  private def getFile[T](job: StreamJob, jobVersion: StreamJobVersion, fileName: String, op: (String, String) => T): T = {
    val streamisFile = findFile(job, jobVersion, fileName)
    streamisFile.getStoreType match {
      case StreamisFile.BML_STORE_TYPE =>
        val resourceMap = JsonUtils.jackson.readValue(streamisFile.getStorePath, classOf[util.Map[String, String]])
        op(resourceMap.get("resourceId"), resourceMap.get("version"))
      case _ =>
        throw new JobExecuteErrorException(30500, s"Not supported storeType ${streamisFile.getStoreType}.")
    }
  }

  protected def getFileContent(job: StreamJob, jobVersion: StreamJobVersion, fileName: String): String =
    getFile(job, jobVersion, fileName, readFileFromBML(jobVersion.getCreateBy, _, _))

  protected def readFile(job: StreamJob, jobVersion: StreamJobVersion, fileName: String): InputStream =
    getFile(job, jobVersion, fileName, readBMLFile(jobVersion.getCreateBy, _, _))

  protected def readBMLFile(userName: String, resourceId: String, version: String): InputStream = {
    if(StringUtils.isBlank(resourceId)) throw new JobExecuteErrorException(30500, "Not exists resourceId.")
    bmlService.get(userName, resourceId, version)
  }

  protected def readFileFromBML(userName: String, resourceId: String, version: String): String =
    IOUtils.toString(readBMLFile(userName, resourceId, version), Configuration.BDP_ENCODING.getValue)

  override def canParse(job: StreamJob, jobVersion: StreamJobVersion): Boolean = jobType == job.getJobType

}
object AbstractJobContentParser {

  val PROJECT_FILE_REGEX = "(^[^.]+)-([\\d]+?\\.[\\d]+?\\.[\\d]+)$".r

}