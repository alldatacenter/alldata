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

package com.webank.wedatasphere.streamis.jobmanager.manager.service

import java.util
import java.util.Date
import com.github.pagehelper.PageInfo
import com.webank.wedatasphere.streamis.jobmanager.launcher.conf.JobConfKeyConstants
import com.webank.wedatasphere.streamis.jobmanager.launcher.service.StreamJobConfService
import com.webank.wedatasphere.streamis.jobmanager.manager.alert.AlertLevel
import org.apache.linkis.common.exception.ErrorException
import org.apache.linkis.common.utils.Logging
import com.webank.wedatasphere.streamis.jobmanager.manager.conf.JobConf
import com.webank.wedatasphere.streamis.jobmanager.manager.dao.{StreamAlertMapper, StreamJobMapper, StreamTaskMapper}
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.vo.{QueryJobListVo, TaskCoreNumVo, VersionDetailVo}
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.{MetaJsonInfo, StreamAlertRecord, StreamJob, StreamJobVersion, StreamJobVersionFiles}
import com.webank.wedatasphere.streamis.jobmanager.manager.exception.{JobCreateErrorException, JobFetchErrorException}
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.JobContentParser
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.entity.StreamisTransformJobContent
import com.webank.wedatasphere.streamis.jobmanager.manager.util.{ReaderUtils, ZipHelper}
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import javax.annotation.Resource
import scala.collection.JavaConverters._


@Service
class DefaultStreamJobService extends StreamJobService with Logging {

  @Autowired
  private var streamJobMapper: StreamJobMapper = _
  @Autowired
  private var streamTaskMapper: StreamTaskMapper = _
  @Autowired
  private var bmlService: BMLService = _
  @Autowired
  private var jobContentParsers: Array[JobContentParser] = _
  @Autowired
  private var streamJobConfService: StreamJobConfService = _
  @Autowired
  private var streamAlertMapper:StreamAlertMapper = _

  override def getJobById(jobId: Long): StreamJob = {
    this.streamJobMapper.getJobById(jobId)
  }

  override def getByProList(projectName: String, userName: String, jobName: String, jobStatus: Integer, jobCreator: String): PageInfo[QueryJobListVo] = {
    val streamJobList = streamJobMapper.getJobLists(projectName, userName, jobName, jobStatus, jobCreator)
    if (streamJobList != null && !streamJobList.isEmpty) {
      val pageInfo = new PageInfo[QueryJobListVo](streamJobList)
      return pageInfo
    }
    new PageInfo[QueryJobListVo](new util.ArrayList[QueryJobListVo]())
  }

  /**
   * COre indicator(核心指标)
   */
  override def countByCores(projectName: String, userName: String): TaskCoreNumVo = {
    val jobs = streamJobMapper.getJobLists(projectName, userName, null, null, null)
    val taskNum = new TaskCoreNumVo()
    taskNum.setProjectName(projectName)
    if (jobs != null && !jobs.isEmpty) {
      jobs.asScala.filter(_.getStatus != null).groupBy(_.getStatus).map(m => (m._1, m._2.size)).foreach(fo => {
        if (fo._1.equals(JobConf.FLINK_JOB_STATUS_COMPLETED.getValue)) taskNum.setSuccessNum(fo._2)
        else if (fo._1.equals(JobConf.FLINK_JOB_STATUS_WAIT_RESTART.getValue)) taskNum.setWaitRestartNum(fo._2)
        else if (fo._1.equals(JobConf.FLINK_JOB_STATUS_ALERT_RUNNING.getValue)) taskNum.setAlertNum(fo._2)
        else if (fo._1.equals(JobConf.FLINK_JOB_STATUS_SLOW_RUNNING.getValue)) taskNum.setSlowTaskNum(fo._2)
        else if (fo._1.equals(JobConf.FLINK_JOB_STATUS_RUNNING.getValue)) taskNum.setRunningNum(fo._2)
        else if (fo._1.equals(JobConf.FLINK_JOB_STATUS_FAILED.getValue)) taskNum.setFailureNum(fo._2)
        else if (fo._1.equals(JobConf.FLINK_JOB_STATUS_STOPPED.getValue)) taskNum.setStoppedNum(fo._2)
      })
    }
    taskNum
  }

  /**
   * job version detail(任务版本详情)
   *
   * @param jobId
   */
  override def versionDetail(jobId: Long, version: String): VersionDetailVo = {
    streamJobMapper.getVersionDetail(jobId, version)
  }


  override def updateVersion(preVersion: String): String = {
    val newVersion = preVersion.substring(1).toInt + 1
    val codeFormat = "%05d"
    "v" + String.format(codeFormat, new Integer(newVersion))
  }

  override def uploadFiles(metaJsonInfo: MetaJsonInfo, version: StreamJobVersion, path: String): Unit = {
    val readerUtils = new ReaderUtils
    readerUtils.listFiles(path).asScala.foreach(path => {
      val response = bmlService.upload(version.getCreateBy, path)
      val jobVersionFiles = new StreamJobVersionFiles
      jobVersionFiles.setJobId(version.getJobId)
      jobVersionFiles.setJobVersionId(version.getId)
      jobVersionFiles.setCreateBy(version.getCreateBy)
      jobVersionFiles.setVersion(version.getVersion)
      jobVersionFiles.setFileName(readerUtils.getFileName(path))
      jobVersionFiles.setCreateTime(new Date(System.currentTimeMillis()))
      jobVersionFiles.setStorePath(readerUtils.readAsJson(response.get("version").toString, response.get("resourceId").toString))
      streamJobMapper.insertJobVersionFiles(jobVersionFiles)
    })
  }


  override def createStreamJob(metaJsonInfo: MetaJsonInfo, userName: String): StreamJobVersion = {
    if(StringUtils.isBlank(metaJsonInfo.getJobType))
      throw new JobCreateErrorException(30030, s"jobType is needed.")
    if(metaJsonInfo.getJobContent == null || metaJsonInfo.getJobContent.isEmpty)
      throw new JobCreateErrorException(30030, s"jobContent is needed.")
    val job = streamJobMapper.getCurrentJob(metaJsonInfo.getProjectName, metaJsonInfo.getJobName)
    val streamJob = new StreamJob()
    val jobVersion = new StreamJobVersion()
    if (job == null) {
      streamJob.setCreateBy(userName)
      streamJob.setSubmitUser(userName)
      streamJob.setJobType(metaJsonInfo.getJobType)
      streamJob.setDescription(metaJsonInfo.getDescription)
      jobVersion.setVersion("v00001")
      streamJob.setCreateTime(new Date())
      streamJob.setLabel(metaJsonInfo.getTags)
      streamJob.setName(metaJsonInfo.getJobName)
      streamJob.setProjectName(metaJsonInfo.getProjectName)
      streamJobMapper.insertJob(streamJob)
    } else {
      if(job.getJobType != metaJsonInfo.getJobType)
        throw new JobCreateErrorException(30030, s"StreamJob-${job.getName} has already created with jobType ${job.getJobType}, you cannot change it to ${metaJsonInfo.getJobType}.")
      streamJob.setId(job.getId)
      if (StringUtils.isNotEmpty(metaJsonInfo.getDescription))
        streamJob.setDescription(metaJsonInfo.getDescription)
      streamJobMapper.updateJob(streamJob)
      val jobVersions = streamJobMapper.getJobVersions(job.getId)
      if (jobVersions == null || jobVersions.isEmpty) jobVersion.setVersion("v00001")
      else
        jobVersion.setVersion(updateVersion(jobVersions.get(0).getVersion))
    }
    jobVersion.setJobId(streamJob.getId)
    jobVersion.setJobContent(metaJsonInfo.getMetaInfo)
    jobVersion.setCreateBy(userName)
    jobVersion.setCreateTime(new Date)
    jobVersion.setSource("upload by user.")
    if (StringUtils.isNotBlank(metaJsonInfo.getComment))
      jobVersion.setComment(metaJsonInfo.getComment)
    else jobVersion.setComment("upload by user.")
    // Should build unique key using job id and version number, to avoid the duplicate version
    streamJobMapper.insertJobVersion(jobVersion)
    jobVersion
  }

  @throws(classOf[ErrorException])
  @Transactional(rollbackFor = Array(classOf[Exception]))
  override def uploadJob(projectName: String, userName: String, inputZipPath: String): StreamJobVersion = {
    val inputPath = ZipHelper.unzip(inputZipPath)
    val readerUtils = new ReaderUtils
    val metaJsonInfo = readerUtils.parseJson(inputPath)
    if (StringUtils.isNotBlank(projectName) && projectName!=metaJsonInfo.getProjectName) {
      throw new JobCreateErrorException(30030, s"the projectName ${metaJsonInfo.getProjectName} is not matching the project ")
    }
    validateUpload(metaJsonInfo.getProjectName, metaJsonInfo.getJobName, userName)
    //  生成StreamJob，根据StreamJob生成StreamJobVersion
    val version = createStreamJob(metaJsonInfo, userName)
    //  上传所有非meta.json的文件
    uploadFiles(metaJsonInfo, version, inputZipPath)
    version
  }

  @throws(classOf[ErrorException])
  @Transactional(rollbackFor = Array(classOf[Exception]))
  override def createOrUpdate(userName: String, metaJsonInfo: MetaJsonInfo): StreamJobVersion = {
    validateUpload(metaJsonInfo.getProjectName, metaJsonInfo.getJobName, userName)
    val readerUtils = new ReaderUtils
    metaJsonInfo.setMetaInfo(readerUtils.readAsJson(metaJsonInfo))
    createStreamJob(metaJsonInfo, userName)
  }

  override def getJobContent(jobId: Long, version: String): StreamisTransformJobContent = {
    val job = streamJobMapper.getJobById(jobId)
    if(job == null) throw new JobFetchErrorException(30030, s"job is not exists.")
    val jobVersion = if(StringUtils.isBlank(version)) {
      streamJobMapper.getJobVersions(jobId).get(0)
    } else streamJobMapper.getJobVersionById(jobId, version)
    if(jobVersion == null)
      throw new JobFetchErrorException(30030, s"job has no versions.")
    jobContentParsers.find(_.canParse(job, jobVersion)).map(_.parseTo(job, jobVersion))
      .getOrElse(throw new JobFetchErrorException(30030, s"Cannot find a JobContentParser to parse jobContent."))
  }


  override def hasPermission(jobId: Long, username: String): Boolean = {
    hasPermission(this.streamJobMapper.getJobById(jobId), username)
  }

  override def hasPermission(job: StreamJob, username: String): Boolean = {
    Option(job) match {
      case Some(job: StreamJob) =>
        if (!username.equals(job.getCreateBy)){
          Option(this.streamJobConfService.getJobConfValue(job.getId,
            JobConfKeyConstants.AUTHORITY_AUTHOR_VISIBLE.getValue)) match {
            case Some(authors) =>
              authors.split(",").toList.contains(username)
            case _ => false
          }
        } else true
      case None => false
    }
  }

  override def getAlertUsers(job: StreamJob): util.List[String] = {
    val alertUsers = this.streamJobConfService.getJobConfValue(job.getId, JobConfKeyConstants.ALERT_USER.getValue)
    if (StringUtils.isBlank(alertUsers)) return null
    alertUsers.split(",").toList.asJava
  }

  override def getAlertLevel(job: StreamJob): AlertLevel = {
    val level = this.streamJobConfService.getJobConfValue(job.getId, JobConfKeyConstants.ALERT_LEVEL.getValue)
    if (StringUtils.isBlank(level)) return AlertLevel.MINOR
    AlertLevel.valueOf(level)
  }

  override def isCreator(jobId: Long, username: String): Boolean = {
    val job = streamJobMapper.getJobById(jobId)
    if (job == null) return false
    username.equals(job.getCreateBy)
  }

  override def getAlert(username: String, jobId: Long, version: String): util.List[StreamAlertRecord] = {
    val job = streamJobMapper.getJobVersionById(jobId, version)
    if (job == null) return null
    streamAlertMapper.getAlertByJobIdAndVersion(username,jobId,job.getId)
  }

  private def validateUpload(projectName: String, jobName: String, userName: String): Unit = {
    if(StringUtils.isBlank(jobName)) throw new JobCreateErrorException(30030, s"jobName is needed.")
    if(StringUtils.isBlank(projectName)) throw new JobCreateErrorException(30030, s"projectName is needed.")
    // Try to lock the stream job to create version
    Option(streamJobMapper.queryAndLockJobInCondition(projectName, jobName)).foreach(streamJob => {
      // Use the project privilege at restful api
//      if (streamJob.getCreateBy != userName)
//        throw new JobCreateErrorException(30030, s"You have no permission to update StreamJob-$jobName.")
      val jobVersions = streamJobMapper.getJobVersions(streamJob.getId)
      if (jobVersions != null && !jobVersions.isEmpty) {
        val tasks = streamTaskMapper.getTasksByJobIdAndJobVersionId(streamJob.getId, jobVersions.get(0).getId)
        if (tasks != null && !tasks.isEmpty && !JobConf.isCompleted(tasks.get(0).getStatus)) {
          throw new JobCreateErrorException(30030, s"StreamJob-$jobName is in status ${tasks.get(0).getStatus}, you cannot upload the zip.")
        }
      }
    })
  }

}
